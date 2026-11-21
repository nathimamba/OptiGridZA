package com.optigridza.predictionservice.service;

import ai.onnxruntime.*;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.nio.FloatBuffer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class OnnxModelService {

    // Must match sklearn LabelEncoder's alphabetical class order exactly.
    // Confirm with print(label_encoder.classes_) in the training notebook.
    private static final String[] CLASS_LABELS = {"CHARGE", "DISCHARGE", "HOLD", "SOLAR_PRIORITY"};

    private OrtEnvironment env;
    private OrtSession session;
    private boolean modelLoaded = false;

    @PostConstruct
    public void loadModel() {
        try {
            env = OrtEnvironment.getEnvironment();
            ClassPathResource resource = new ClassPathResource("model/optigrid_model.onnx");
            try (InputStream is = resource.getInputStream()) {
                byte[] modelBytes = is.readAllBytes();
                session = env.createSession(modelBytes, new OrtSession.SessionOptions());
            }
            modelLoaded = true;
            log.info("ONNX model loaded successfully — {} classes configured.", CLASS_LABELS.length);
            warmUp();
        } catch (Exception e) {
            modelLoaded = false;
            log.error("Failed to load ONNX model — will fall back to safe HOLD per NFR-03: {}", e.getMessage());
        }
    }

    /** One throw-away inference at start-up so the first real request does not pay the JIT/session cost. */
    private void warmUp() {
        try {
            predict(new float[] {0f, 0f, 1.5f, 3f, 50f, 3.5f, 0f});
            predict(new float[] {5f, 0.1f, 1.5f, 3f, 50f, 3.5f, 0f});
        } catch (Exception e) {
            log.warn("ONNX warm-up skipped: {}", e.getMessage());
        }
    }

    public boolean isModelLoaded() {
        return modelLoaded;
    }

    public PredictionResult predict(float[] features) {
        if (!modelLoaded) return null;

        try {
            long[] shape = {1, features.length};
            try (OnnxTensor inputTensor = OnnxTensor.createTensor(env, FloatBuffer.wrap(features), shape)) {

                Map<String, OnnxTensor> inputs = new HashMap<>();
                inputs.put("float_input", inputTensor);

                long startNs = System.nanoTime();
                try (OrtSession.Result result = session.run(inputs)) {
                    long inferenceUs = (System.nanoTime() - startNs) / 1000;
                    log.info("ONNX inference_us={}", inferenceUs);

                    long[] labelArr = (long[]) result.get(0).getValue();
                    int labelIndex = (int) labelArr[0];
                    String action = CLASS_LABELS[labelIndex];

                    double confidence = 0.6;
                    Object probObj = result.get(1).getValue();
                    if (probObj instanceof List<?> probList && !probList.isEmpty()) {
                        Object first = probList.get(0);
                        // ONNX Runtime returns a sequence of OnnxMap objects (not java.util.Map)
                        Map<?, ?> probMap = null;
                        if (first instanceof OnnxMap onnxMap) {
                            probMap = onnxMap.getValue();
                        } else if (first instanceof Map<?, ?> plainMap) {
                            probMap = plainMap;
                        }
                        if (probMap != null) {
                            Object p = probMap.get((long) labelIndex);
                            if (p instanceof Number n) confidence = n.doubleValue();
                        } else {
                            log.warn("Unexpected probability type {}; using default confidence", first == null ? "null" : first.getClass().getName());
                        }
                    } else if (probObj instanceof float[][] probs && probs.length > 0) {
                        confidence = probs[0][labelIndex];
                    }

                    return new PredictionResult(action, confidence);
                }
            }
        } catch (OrtException e) {
            log.error("ONNX inference failed — falling back to safe HOLD per NFR-03: {}", e.getMessage());
            return null;
        }
    }

    public record PredictionResult(String action, double confidence) {}
}
