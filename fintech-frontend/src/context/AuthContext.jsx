import { createContext, useContext, useState, useEffect } from 'react'

const AuthContext = createContext(null)

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null)
  const [token, setToken] = useState(null)
  const [role, setRole] = useState(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    const savedToken = localStorage.getItem('fintech_token')
    const savedUser  = localStorage.getItem('fintech_user')
    const savedRole  = localStorage.getItem('fintech_role')
    if (savedToken && savedUser) {
      setToken(savedToken)
      setUser(savedUser)
      setRole(savedRole || 'USER')
    }
    setLoading(false)
  }, [])

  const login = (tok, username, userRole) => {
    localStorage.setItem('fintech_token', tok)
    localStorage.setItem('fintech_user', username)
    localStorage.setItem('fintech_role', userRole || 'USER')
    setToken(tok)
    setUser(username)
    setRole(userRole || 'USER')
  }

  const logout = () => {
    localStorage.removeItem('fintech_token')
    localStorage.removeItem('fintech_user')
    localStorage.removeItem('fintech_role')
    setToken(null)
    setUser(null)
    setRole(null)
  }

  return (
    <AuthContext.Provider value={{ user, token, role, login, logout, loading }}>
      {children}
    </AuthContext.Provider>
  )
}

export const useAuth = () => useContext(AuthContext)
