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
        } catch (Exception e) {
            modelLoaded = false;
            log.error("Failed to load ONNX model — will fall back to safe HOLD per NFR-03: {}", e.getMessage());
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

                try (OrtSession.Result result = session.run(inputs)) {

                    long[] labelArr = (long[]) result.get(0).getValue();
                    int labelIndex = (int) labelArr[0];
                    String action = CLASS_LABELS[labelIndex];

                    double confidence = 0.6;
                    Object probObj = result.get(1).getValue();
                    if (probObj instanceof List<?> probList && !probList.isEmpty()) {
                        Object first = probList.get(0);
                        if (first instanceof Map<?, ?> probMap) {
                            Object p = probMap.get((long) labelIndex);
                            if (p instanceof Float f) confidence = f;
                        }
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