import axios from 'axios'

// Global Error Interceptor -> Destructures the API Response to flat {status, message, data}
const responseInterceptor = (response) => {
  // If the backend returned ApiResponse envelope
  if (response.data && response.data.status) {
    return response.data
  }
  return response.data
}

const errorInterceptor = (error) => {
  if (error.response && error.response.data) {
    return Promise.reject(error.response.data)
  }
  return Promise.reject({ status: 'error', message: error.message })
}

// Attach Bearer token to every request
const authInterceptor = (token) => (config) => {
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
}

const createSecureClient = (token, baseURL) => {
  const api = axios.create({ baseURL })
  api.interceptors.request.use(authInterceptor(token))
  api.interceptors.response.use(responseInterceptor, errorInterceptor)
  return api
}

// ── Auth ──────────────────────────────────
export const authService = {
  createClient: () => {
    const api = axios.create({ baseURL: '/api/auth' })
    api.interceptors.response.use(responseInterceptor, errorInterceptor)
    return api
  },
  register: (username, password) =>
    authService.createClient().post('/register', { username, password }),
  login: (username, password) =>
    authService.createClient().post('/login', { username, password }),
}

// ── Accounts ──────────────────────────────
export const accountService = (token) => {
  const api = createSecureClient(token, '/api/accounts')
  return {
    create:      (data)        => api.post('', data),
    getMe:       ()            => api.get(`/me`),
    getById:     (id)          => api.get(`/${id}`),
    deposit:     (id, amount)  => api.put(`/${id}/deposit?amount=${amount}`),
    withdraw:    (id, amount)  => api.put(`/${id}/withdraw?amount=${amount}`),
  }
}

// ── Transactions ──────────────────────────
export const transactionService = (token) => {
  const api = createSecureClient(token, '/api/transactions')
  return {
    transfer:       (data)      => api.post('/transfer', data),
    getMy:          ()          => api.get(`/my`),
    getAll:         ()          => api.get(''),
  }
}
