// Funções de utilidade para autenticação do painel de admin

/**
 * Salva o token JWT no armazenamento local do navegador.
 * @param {string} token O token JWT recebido da API.
 */
function saveToken(token) {
    localStorage.setItem('admin_jwt', token);
}

/**
 * Recupera o token JWT do armazenamento local.
 * @returns {string|null} O token JWT ou null se não existir.
 */
function getToken() {
    return localStorage.getItem('admin_jwt');
}

/**
 * Remove o token do armazenamento local e redireciona para a página de login.
 */
function logout() {
    localStorage.removeItem('admin_jwt');
    window.location.href = 'login.html';
}

/**
 * Protege uma página, verificando se um token JWT existe.
 * Se não houver token, redireciona o usuário para a página de login.
 */
function protectPage() {
    const token = getToken();
    if (!token) {
        console.log("Nenhum token encontrado, redirecionando para o login.");
        window.location.href = 'login.html';
    }
    // Futuramente, podemos adicionar uma verificação para ver se o token não expirou.
}

/**
 * Retorna os cabeçalhos de autorização para serem usados em requisições fetch.
 * @returns {Headers} Um objeto Headers com o token de autorização.
 */
function getAuthHeaders() {
    const headers = new Headers({ 'Content-Type': 'application/json' });
    const token = getToken();
    if (token) {
        headers.append('Authorization', `Bearer ${token}`);
    }
    return headers;
}