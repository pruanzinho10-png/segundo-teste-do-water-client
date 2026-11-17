const $ = (selector, root = document) => root.querySelector(selector);
const $$ = (selector, root = document) => [...root.querySelectorAll(selector)];
const welcome = $('#welcome');
const premium = $('#premiumScreen');
const offline = $('#offlineScreen');
const dashboard = $('#dashboard');
const content = $('#content');
const toast = $('#toast');
const store = {
  get() { try { return JSON.parse(localStorage.getItem('water-preview') || '{}'); } catch { return {}; } },
  set(values) { try { localStorage.setItem('water-preview', JSON.stringify({ ...this.get(), ...values })); } catch { /* a prévia segue sem persistência se o navegador bloquear storage local */ } }
};
let state = { page: 'Início', profile: store.get().profile || 'Player123', type: 'Offline', motion: store.get().motion || 'Reduzida', query: '' };
let toastTimer;

const mods = [
  ['Sodium','Otimização de renderização para Fabric.','Fabric · 1.21.1 · 1.21.11','↗','2.4M'],
  ['Iris Shaders','Suporte a shaders com Sodium.','Fabric · 1.21.1 · 1.21.11','◈','1.8M'],
  ['Lithium','Otimizações de lógica do jogo.','Fabric · Quilt · 1.21.1','⌁','980k'],
  ['Mod Menu','Gerencie mods pelo próprio Minecraft.','Fabric · 1.21.1 · 1.21.11','▤','670k']
];
const navPages = ['Início','Versões','Mods','Servidores','Downloads','Contas','Configurações'];

function notify(message) {
  toast.textContent = message; toast.classList.add('show'); clearTimeout(toastTimer);
  toastTimer = setTimeout(() => toast.classList.remove('show'), 2400);
}
function showOnly(target) {
  [welcome, premium, offline, dashboard].forEach(section => section.classList.add('hidden'));
  target.classList.remove('hidden');
}
function enterDashboard(profile = state.profile) {
  state.profile = profile; state.type = 'Offline'; store.set({ profile });
  $('#activeName').textContent = profile; $('.avatar').textContent = profile.slice(0,1).toUpperCase();
  $('#activeType').textContent = 'Offline';
  showOnly(dashboard); renderPage(state.page);
}

$('#premiumChoice').addEventListener('click', () => showOnly(premium));
$('#offlineChoice').addEventListener('click', () => showOnly(offline));
$('#skipWelcome').addEventListener('click', () => enterDashboard('Player123'));
$$('[data-back]').forEach(button => button.addEventListener('click', () => showOnly(welcome)));
$('#offlineForm').addEventListener('submit', event => {
  event.preventDefault(); const name = $('#offlineName').value.trim();
  if (!/^[A-Za-z0-9_]{3,16}$/.test(name)) { notify('Use de 3 a 16 letras, números ou _.'); return; }
  const accounts = store.get().accounts || [];
  if (!accounts.includes(name)) accounts.push(name);
  store.set({ accounts, profile: name }); enterDashboard(name);
});
$$('[data-page]').forEach(button => button.addEventListener('click', () => {
  state.page = button.dataset.page; enterDashboard(state.profile);
}));
$('#profileChip').addEventListener('click', () => { state.page = 'Contas'; renderPage('Contas'); });

function pageHeader(title, desc, action = '') {
  return `<div class="page-head"><div><h2>${title}</h2><p>${desc}</p></div>${action}</div>`;
}
function home() {
  return `${pageHeader('Pronto para jogar?','Olá, '+escapeHtml(state.profile)+'. Sua jornada começa aqui.','<span class="tag">PERFIL OFFLINE</span>')}
    <section class="home-hero"><div class="hero-top"><span class="installed">●  PERFIL LOCAL</span><span class="fabric">Minecraft Java · Demo</span></div><h3>WATER CLIENT</h3><p class="hero-sub">O client mais limpo para celulares.</p><div class="play-row"><button class="play-button" data-action="play">▷ &nbsp; JOGAR</button><button class="launch-select" data-page="Versões">Vanilla 1.21.1　⌄</button></div></section>
    <div class="grid-two"><button class="mini-card" data-page="Mods"><span class="mini-icon">✣</span><span><strong>Biblioteca de mods</strong><small>Explore projetos do Modrinth</small></span></button><button class="mini-card" data-page="Servidores"><span class="mini-icon">▣</span><span><strong>Servidores</strong><small>Contas Premium e Offline</small></span></button></div>
    <div class="status-row"><span><i></i> Modo demonstração</span><span>Versão do launcher 0.1.0</span><span>Runtime de jogo não conectado</span></div>`;
}
function modPage() {
  const filtered = mods.filter(m => m.join(' ').toLowerCase().includes(state.query.toLowerCase()));
  const cards = filtered.map((m,i) => `<article class="panel mod-card"><span class="mod-icon">${m[3]}</span><div class="mod-copy"><strong>${m[0]}</strong><p>${m[1]}</p><div class="mod-meta"><span>${m[2]}</span><span>↓ ${m[4]}</span></div><button class="mod-action" data-action="demo-install" data-mod="${m[0]}">Ver detalhes →</button></div></article>`).join('');
  return `${pageHeader('Biblioteca de mods','Catálogo de demonstração · origem marcada em cada projeto.','<span class="tag">MODRINTH · DEMO</span>')}
    <div class="toolbar"><input class="search" id="modSearch" placeholder="Pesquisar mods…" value="${escapeHtml(state.query)}"><select class="select" id="loader"><option>Fabric</option><option>Forge</option><option>NeoForge</option><option>Quilt</option></select><select class="select" id="gameVersion"><option>1.21.1</option><option>1.21.11</option><option>1.20.1</option></select></div>
    <div class="mods-grid">${cards || '<div class="panel empty"><strong>Nenhum projeto encontrado</strong></div>'}</div><p class="subtle" style="margin-top:9px">Itens ilustrativos. A prévia não consulta API nem instala arquivos.</p>`;
}
function versions() {
  return `${pageHeader('Versões e instâncias','Cada instalação terá pasta e configuração próprias.','<button class="secondary-button" data-action="new-version">＋ Nova instalação</button>')}
  <div class="table"><div class="table-row"><strong>Minecraft Java 1.21.1</strong><span>Vanilla</span><span class="pill">DEMONSTRAÇÃO</span><button class="secondary-button" data-action="select-version">Selecionar</button></div><div class="table-row"><strong>Perfil Fabric 1.21.1</strong><span>Fabric</span><span class="pill">EXEMPLO</span><button class="secondary-button" data-action="select-version">Selecionar</button></div><div class="table-row"><strong>NeoForge 1.20.1</strong><span>NeoForge</span><span class="pill">EXEMPLO</span><button class="secondary-button" data-action="select-version">Selecionar</button></div></div><p class="subtle" style="margin-top:10px">Versões ainda não foram baixadas. Selecionar nesta prévia não altera uma instalação real.</p>`;
}
function servers() {
  const servers = store.get().servers || [];
  const rows = servers.map((s,i) => `<div class="table-row"><strong>${escapeHtml(s.name)}</strong><span>${escapeHtml(s.address)}</span><span class="pill">SALVO LOCALMENTE</span><button class="secondary-button" data-remove-server="${i}">Remover</button></div>`).join('');
  return `${pageHeader('Servidores','Salve endereços para usar quando o runtime estiver pronto.','<button class="secondary-button" data-action="add-server">＋ Adicionar servidor</button>')}${rows ? `<div class="table">${rows}</div>` : '<div class="panel empty"><span>▣</span><strong>Sua lista está vazia</strong><small>Adicione um servidor. A conexão de jogo não está disponível nesta prévia.</small></div>'}<p class="subtle" style="margin-top:9px">Offline funciona apenas em servidores que aceitam voluntariamente autenticação offline.</p>`;
}
function downloads() {
  return `${pageHeader('Downloads','Acompanhe instalações e arquivos em um só lugar.','<span class="tag">FILA VAZIA</span>')}<div class="panel empty"><span>↓</span><strong>Nenhum download ativo</strong><small>A demonstração não baixa mods, runtimes nem arquivos do Minecraft.</small></div>`;
}
function accounts() {
  const accounts = store.get().accounts || [];
  const all = [...new Set([state.profile, ...accounts].filter(Boolean))];
  return `${pageHeader('Contas','Gerencie perfis locais e o acesso oficial.','<button class="secondary-button" data-action="new-account">＋ Adicionar conta</button>')}<div class="panel account-row"><span class="avatar">${escapeHtml(state.profile[0] || 'P')}</span><span class="grow"><strong>${escapeHtml(state.profile)}</strong><small class="subtle">Perfil Offline · ativo nesta demonstração</small></span><span class="pill">ATIVO</span></div>${all.filter(n=>n!==state.profile).map(n=>`<div class="panel account-row"><span class="avatar">${escapeHtml(n[0])}</span><span class="grow"><strong>${escapeHtml(n)}</strong><small class="subtle">Perfil Offline local</small></span><button class="secondary-button" data-use-account="${escapeHtml(n)}">Usar</button></div>`).join('')}<div class="panel" style="margin-top:9px"><strong>Minecraft Java Original</strong><p class="subtle" style="margin-top:5px">Login Microsoft oficial ainda não conectado. A prévia nunca pede nem salva sua senha.</p><button class="mod-action" data-action="premium">Sobre a conexão →</button></div>`;
}
function settings() {
  return `${pageHeader('Configurações','A interface preserva suas escolhas nesta prévia.','<span class="tag">LEVE POR PADRÃO</span>')}<div class="settings-layout"><div class="panel"><strong>Animações</strong>${['Completa','Reduzida','Mínima'].map(mode=>`<div class="setting-line"><span>${mode}<small>${mode==='Completa'?'Transições e feedbacks':mode==='Reduzida'?'Movimentos essenciais':'Prioriza resposta e economia'}</small></span><button class="secondary-button" data-motion="${mode}">${state.motion===mode?'Ativa':'Usar'}</button></div>`).join('')}<div class="setting-line"><span>Respeitar reduzir movimento<small>Preferência de acessibilidade do navegador</small></span><span class="pill">AUTOMÁTICO</span></div></div><div class="panel"><strong>Jogo e armazenamento</strong><div class="setting-line"><span>Memória Java<small>Disponível quando runtime integrado</small></span><span class="subtle">—</span></div><div class="setting-line"><span>Renderer gráfico<small>Será detectado no Android</small></span><span class="subtle">—</span></div><div class="setting-line"><span>Cache de mods e imagens<small>Nenhum arquivo salvo</small></span><button class="secondary-button" data-action="clear-cache">Limpar</button></div></div></div>`;
}
const renderers = { 'Início':home,'Mods':modPage,'Versões':versions,'Servidores':servers,'Downloads':downloads,'Contas':accounts,'Configurações':settings };
function renderPage(page) {
  state.page = page; content.innerHTML = `<div class="page-enter">${(renderers[page] || home)()}</div>`;
  $('#launcher').dataset.motion = state.motion.toLowerCase();
  $$('.nav-item').forEach(button => button.classList.toggle('active', button.dataset.page===page));
  const search = $('#modSearch'); if (search) search.addEventListener('input', e => { state.query=e.target.value; renderPage('Mods'); $('#modSearch').focus(); $('#modSearch').setSelectionRange(state.query.length,state.query.length); });
  content.querySelectorAll('[data-action]').forEach(button=>button.addEventListener('click',()=>runAction(button.dataset.action,button.dataset.mod)));
  content.querySelectorAll('[data-page]').forEach(button=>button.addEventListener('click',()=>renderPage(button.dataset.page)));
  content.querySelectorAll('[data-motion]').forEach(button=>button.addEventListener('click',()=>{state.motion=button.dataset.motion;store.set({motion:state.motion});renderPage('Configurações');notify(`Animações: ${state.motion.toLowerCase()}.`)}));
  content.querySelectorAll('[data-use-account]').forEach(button=>button.addEventListener('click',()=>enterDashboard(button.dataset.useAccount)));
  content.querySelectorAll('[data-remove-server]').forEach(button=>button.addEventListener('click',()=>{const list=store.get().servers||[];list.splice(Number(button.dataset.removeServer),1);store.set({servers:list});renderPage('Servidores');}));
}
function runAction(action, name) {
  if(action==='play') notify('Demonstração: runtime Android ainda não conectado.');
  if(action==='demo-install') notify(`${name}: detalhes ilustrativos, sem instalação real.`);
  if(action==='select-version') notify('Prévia: nenhuma versão foi baixada ou selecionada de verdade.');
  if(action==='new-version') notify('Gerenciador real de versões depende do runtime.');
  if(action==='add-server') { const n=prompt('Nome do servidor (demonstração):'); if(!n)return; const a=prompt('Endereço, por exemplo play.exemplo.net:25565');if(!a)return;const list=store.get().servers||[];list.push({name:n,address:a});store.set({servers:list});renderPage('Servidores'); }
  if(action==='new-account') { state.page='Início'; showOnly(welcome); }
  if(action==='premium') notify('OAuth Microsoft oficial ainda precisa ser configurado.');
  if(action==='clear-cache') notify('Nenhum cache foi criado nesta demonstração.');
}
function escapeHtml(value) { return String(value).replace(/[&<>"']/g, c=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c])); }

if (store.get().profile) enterDashboard(store.get().profile);
