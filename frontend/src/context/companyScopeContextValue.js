import { createContext, useContext } from 'react'

export const CompanyScopeContext = createContext(null)
export const useCompanyScope = () => useContext(CompanyScopeContext)
