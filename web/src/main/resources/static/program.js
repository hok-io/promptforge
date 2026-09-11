(() => {
  'use strict';
  const $ = selector => document.querySelector(selector);
  const state = { name: 'PromptForge', resourceRevision: 'new', resourceDirty: false, resourceSavedPath: '', revision: 'new', outline: { steps: [] }, saved: null, preview: null, selected: null, tab: 'main', dirty: false, csrf: null };
  const groups = [
    ['這一步要做甚麼', [['id', '步驟 ID', 'text'], ['title', '名稱', 'text'], ['purpose', '目的', 'area'], ['instructions', '指示', 'area']]],
    ['ReAct：判斷與執行', [['input', '輸入', 'area'], ['decision', '判斷依據', 'area'], ['actions', '可採取的行動', 'area'], ['observation', '預期觀察', 'area'], ['adjustment', '失敗後如何調整', 'area']]],
    ['結果與停止', [['output', '輸出', 'area'], ['stop', '停止條件', 'area']]]
  ];
  function element(tag, className, text) { const node = document.createElement(tag); if (className) node.className = className; if (text !== undefined) node.textContent = text; return node; }
  function message(text, error = false) { $('#message').textContent = text; $('#message').classList.toggle('error', error); }
  function markDirty() { state.dirty = true; state.preview = null; updateSummary(); showOutput(); }
  function updateSummary() {
    $('#stepCount').textContent = String(state.outline.steps.length);
    $('#selectedCount').textContent = String(state.outline.steps.filter(step => step.selected).length);
    $('#saveState').textContent = state.dirty ? '尚未保存' : state.saved?.generatedCurrent ? '已同步' : '需重新生成';
    $('#revision').textContent = state.revision === 'new' ? '新大綱' : `版本 ${state.revision.slice(0, 10)}`;
  }
  function migrateLegacy(step) {
    step.capabilities ||= [];
    for (const [key, type] of [['scripts', 'script'], ['references', 'reference'], ['skills', 'skill']]) {
      if (Array.isArray(step[key]) && step[key].length) {
        step.capabilities.push(...step[key].map(path => ({ type, path, selected: true })));
        step[key] = [];
        state.dirty = true;
      }
    }
  }
  function renderSteps() {
    const list = $('#steps'); list.replaceChildren();
    state.outline.steps.forEach((step, index) => {
      const row = element('div', `step-card${step === state.selected ? ' active' : ''}`);
      const top = element('div', 'step-top');
      const open = element('button', 'step-open'); open.type = 'button'; open.append(document.createTextNode(step.title || '未命名步驟'), element('small', '', step.id || '未設定 ID')); open.onclick = () => { state.selected = step; renderSteps(); renderEditor(); };
      const selected = element('label', 'selected-label'); const check = element('input'); check.type = 'checkbox'; check.checked = !!step.selected; check.setAttribute('aria-label', `${step.title || step.id} 提供給 Agent`); check.onchange = () => { step.selected = check.checked; markDirty(); }; selected.append(check, document.createTextNode('啟用'));
      top.append(open, selected); row.append(top);
      const controls = element('div', 'step-controls');
      for (const [label, delta] of [['上移', -1], ['下移', 1]]) {
        const button = element('button', '', label); button.type = 'button'; button.disabled = !state.outline.steps[index + delta]; button.onclick = () => { [state.outline.steps[index], state.outline.steps[index + delta]] = [state.outline.steps[index + delta], state.outline.steps[index]]; markDirty(); renderSteps(); };
        controls.append(button);
      }
      const remove = element('button', '', '刪除'); remove.type = 'button'; remove.onclick = () => { if (!confirm(`從管理大綱刪除「${step.title || step.id}」？`)) return; state.outline.steps.splice(index, 1); if (state.selected === step) state.selected = state.outline.steps[Math.min(index, state.outline.steps.length - 1)] || null; markDirty(); renderSteps(); renderEditor(); }; controls.append(remove);
      row.append(controls); list.append(row);
    });
    if (!state.outline.steps.length) list.append(element('p', 'empty', '尚無步驟。按「新增步驟」開始。'));
  }
  function renderEditor() {
    const editor = $('#editor'); editor.replaceChildren(); const step = state.selected;
    if (!step) { editor.append(element('p', 'empty', '選一個步驟開始編輯。')); return; }
    for (const [title, fields] of groups) {
      editor.append(element('h3', '', title)); const grid = element('div', 'field-grid');
      for (const [key, labelText, kind] of fields) {
        const label = element('label', `field${kind === 'area' ? ' wide' : ''}`, labelText);
        const input = element(kind === 'area' ? 'textarea' : 'input'); if (kind === 'text') input.type = 'text'; input.value = step[key] || ''; input.placeholder = key === 'id' ? '小寫英文、數字與連字號' : '';
        input.oninput = () => { step[key] = input.value; markDirty(); if (key === 'title' || key === 'id') renderSteps(); };
        label.append(input); grid.append(label);
      }
      editor.append(grid);
    }
    editor.append(element('h3', '', '可選能力與依賴'), element('p', 'subtle', '未勾選能力保留在大綱，但不放入 Agent 版本；已勾選檔案缺失時 Save 會報錯。'));
    const list = element('div', 'capability-list'); editor.append(list); step.capabilities ||= [];
    step.capabilities.forEach((cap, index) => {
      const row = element('div', 'capability'); const selected = element('label'); const check = element('input'); check.type = 'checkbox'; check.checked = !!cap.selected; check.setAttribute('aria-label', `能力 ${index + 1} 提供給 Agent`); check.onchange = () => { cap.selected = check.checked; markDirty(); }; selected.append(check, document.createTextNode('啟用'));
      const type = element('select'); type.setAttribute('aria-label', `能力 ${index + 1} 類型`); for (const value of ['script', 'reference', 'skill', 'asset']) { const option = element('option', '', ({script:'腳本',reference:'參考資料',skill:'其他 Skill',asset:'素材'})[value]); option.value = value; type.append(option); } type.value = cap.type || 'reference'; type.onchange = () => { cap.type = type.value; markDirty(); };
      const path = element('input'); path.type = 'text'; path.setAttribute('list', 'skillPaths'); path.placeholder = 'repo 相對檔案路徑'; path.value = cap.path || ''; path.setAttribute('aria-label', `能力 ${index + 1} 路徑`); path.oninput = () => { cap.path = path.value; markDirty(); };
      const remove = element('button', '', '移除'); remove.type = 'button'; remove.onclick = () => { step.capabilities.splice(index, 1); markDirty(); renderEditor(); };
      const usage = element('input'); usage.placeholder = '使用方法／執行命令'; usage.value = cap.usage || ''; usage.setAttribute('aria-label', '能力使用方法'); usage.oninput = () => { cap.usage = usage.value; markDirty(); };
      const deps = element('input'); deps.placeholder = '必要依賴路徑，以逗號分隔'; deps.value = (cap.requires || []).join(', '); deps.setAttribute('aria-label', '能力必要依賴'); deps.oninput = () => { cap.requires = deps.value.split(',').map(x => x.trim()).filter(Boolean); markDirty(); };
      row.append(selected, type, path, usage, deps, remove); list.append(row);
    });
    const add = element('button', '', '＋ 新增能力'); add.type = 'button'; add.onclick = () => { step.capabilities.push({ type: 'reference', path: '', selected: false }); markDirty(); renderEditor(); }; editor.append(add);
  }
  function showOutput() {
    const value = state.preview || state.saved || {};
    $('#generated').textContent = state.tab === 'main' ? value.generated || '尚未生成' : value.contextGenerated || '尚未生成';
    $('#outputHint').textContent = state.preview ? '這是目前選擇的預覽，尚未保存到 Agent 版本。' : state.dirty ? '目前顯示上次保存的版本；大綱有尚未保存的修改。' : '這裏顯示目前已保存的 Agent 版本。';
    for (const [name, button] of [['main', '#tabMain'], ['context', '#tabContext']]) { $(button).classList.toggle('active', state.tab === name); $(button).setAttribute('aria-selected', String(state.tab === name)); }
  }
  async function request(path, options = {}) {
    const headers = new Headers(options.headers || {});
    if (options.body !== undefined) headers.set('Content-Type', 'application/json');
    if (options.method && options.method !== 'GET') {
      if (!state.csrf) { const response = await fetch('/api/csrf'); if (!response.ok) throw Error('無法取得本機保存權限。'); state.csrf = await response.json(); }
      headers.set(state.csrf.headerName, state.csrf.token);
    }
    const response = await fetch(path, { ...options, headers }); const data = await response.json().catch(() => null);
    if (!response.ok) { const error = Error(data?.message || `操作失敗 (${response.status})`); error.status = response.status; throw error; }
    return data;
  }
  async function load() {
    const data = await request(endpoint()); state.revision = data.revision; state.outline = data.outline; state.saved = data; state.preview = null; state.dirty = false;
    const id = state.selected?.id; state.outline.steps.forEach(migrateLegacy); state.selected = state.outline.steps.find(step => step.id === id) || state.outline.steps[0] || null;
    $('#skillDescription').value = state.outline.description || (state.name === 'PromptForge' ? "Turn a user's task into an executable prompt using the selected local PromptForge outline." : ''); $('#tabMain').textContent = state.name; $('#tabContext').hidden = state.name !== 'PromptForge'; state.tab = 'main'; $('#skillLocation').textContent = `生成位置：.agents/skills/${state.name}/SKILL.md`; renderSteps(); renderEditor(); updateSummary(); showOutput(); message(data.status); await refreshResources(); checkMcp();
  }
  async function preview() {
    const button = $('#preview'); button.disabled = true;
    try { state.preview = await request(endpoint() + '/preview', { method: 'POST', body: JSON.stringify(state.outline) }); showOutput(); message('預覽成功；尚未保存。'); $('#output').scrollIntoView({ behavior: 'smooth' }); }
    catch (error) { message(error.message, true); } finally { button.disabled = false; }
  }
  async function save() {
    const button = $('#save'); button.disabled = true;
    try {
      const data = await request(endpoint(), { method: 'PUT', body: JSON.stringify({ revision: state.revision, outline: state.outline }) });
      state.revision = data.revision; state.saved = data; state.preview = null; state.dirty = false; updateSummary(); showOutput(); message(data.status); await refreshSkills(); checkMcp();
    } catch (error) { message(error.status === 409 ? `${error.message} 請按「重新讀取磁碟」取得新版。` : error.message, true); }
    finally { button.disabled = false; }
  }
  async function checkMcp() {
    $('#mcpState').textContent = '檢查中'; $('#mcpUrl').textContent = `${location.origin}/mcp`;
    $('#mcpCommand').textContent = `codex mcp add promptforge --url ${location.origin}/mcp`; $('#mcpConfig').textContent = `[mcp_servers.promptforge]\nurl = "${location.origin}/mcp"`; $('#agentPrompt').textContent = `請實際呼叫 promptforge 的 list_skills，再用 get_skill 讀取 ${state.name}，告訴我它有哪些步驟。先不要執行任務。`; $('#directSkill').textContent = `在此 project 的新工作階段使用 ${'$'}${state.name}。PromptForge 產生的 prompt 可直接交給 $PromptForge-context。`;
    try {
      const response = await fetch('/mcp', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ jsonrpc: '2.0', id: 1, method: 'tools/call', params: { name: 'get_skill', arguments: {name: state.name} } }) });
      if (!response.ok) throw Error(`HTTP ${response.status}`);
      const result = await response.json(); const returned = result.result?.content?.[0]?.text;
      if (!returned || result.result?.isError) throw Error(returned || 'MCP 沒有回傳生成版');
      const same = returned === state.saved?.generated;
      $('#mcpState').textContent = same ? '可連線' : '版本不同';
      $('#mcpResult').textContent = same ? 'MCP 回傳與已保存生成版一致。' : 'MCP 內容與 Web 顯示不同，請重新讀取。';
    } catch (error) { $('#mcpState').textContent = '無法連線'; $('#mcpResult').textContent = error.message; }
  }
  $('#addStep').onclick = () => { const step = { id: `step-${Date.now().toString(36)}`, title: '新步驟', selected: false, capabilities: [] }; state.outline.steps.push(step); state.selected = step; markDirty(); renderSteps(); renderEditor(); };
  $('#preview').onclick = preview; $('#save').onclick = save; $('#checkMcp').onclick = checkMcp;
  $('#reload').onclick = () => { if ((state.dirty || state.resourceDirty) && !confirm('捨棄尚未保存的修改，重新讀取磁碟？')) return; clearResource(); load().catch(error => message(error.message, true)); };
  $('#tabMain').onclick = () => { state.tab = 'main'; showOutput(); }; $('#tabContext').onclick = () => { state.tab = 'context'; showOutput(); };
  window.addEventListener('beforeunload', event => { if (state.dirty || state.resourceDirty) { event.preventDefault(); event.returnValue = ''; } });

  function endpoint() { return '/api/skills/' + encodeURIComponent(state.name); }
  async function refreshSkills() {
    const names = await request('/api/skills'); if (!names.includes(state.name)) names.push(state.name);
    $('#skillSelect').replaceChildren(...names.map(name => { const option = element('option', '', name); option.value = name; return option; })); $('#skillSelect').value = state.name; $('#skillPaths').replaceChildren(...[...names, 'PromptForge-context'].map(name => { const option = element('option'); option.value = `.agents/skills/${name}/SKILL.md`; return option; }));
  }
  async function refreshResources() {
    const paths = await request(endpoint() + '/files');
    $('#resourceSelect').replaceChildren(element('option', '', '新增檔案')); $('#resourceSelect').firstChild.value = '';
    for (const path of paths) { const local = path.split('/').slice(3).join('/'); const option = element('option', '', local); option.value = local; $('#resourceSelect').append(option); }
  }
  function clearResource() { state.resourceRevision = 'new'; state.resourceDirty = false; state.resourceSavedPath = ''; $('#resourcePath').value = ''; $('#resourcePath').readOnly = false; $('#resourceText').value = ''; $('#attachResource').disabled = true; $('#resourceStatus').textContent = ''; }
  const guarded = action => async () => { try { await action(); } catch(error) { message(error.message, true); } };
  $('#skillDescription').oninput = () => { state.outline.description = $('#skillDescription').value; markDirty(); };
  $('#skillSelect').onchange = guarded(async () => {
    const next = $('#skillSelect').value;
    if ((state.dirty || state.resourceDirty) && !confirm('切換 Skill 會捨棄未保存的修改，繼續？')) { $('#skillSelect').value = state.name; return; }
    state.name = next; clearResource(); await load();
  });
  $('#newSkill').onclick = () => { $('#newSkillForm').hidden = false; $('#newSkillId').focus(); };
  $('#cancelNewSkill').onclick = () => { $('#newSkillForm').hidden = true; };
  $('#createSkill').onclick = guarded(async () => {
    const name = $('#newSkillId').value.trim();
    if (!/^[a-z][a-z0-9-]{0,62}$/.test(name)) throw Error('請用小寫英文、數字、連字號，最多 63 字元。');
    if ((await request('/api/skills')).includes(name)) throw Error('已有同名 Skill，請從清單選擇。');
    if ((state.dirty || state.resourceDirty) && !confirm('建立新 Skill 會捨棄目前未保存修改，繼續？')) return;
    state.name = name; clearResource(); await refreshSkills(); await load(); $('#newSkillForm').hidden = true;
    $('#addStep').click(); state.selected.selected = true; renderSteps(); updateSummary(); message('新 Skill 草稿：填寫用途與步驟，再按「保存並生成 Skill」。');
  });
  $('#resourceSelect').onchange = guarded(async () => {
    const path = $('#resourceSelect').value;
    if (state.resourceDirty && !confirm('捨棄未保存的檔案修改？')) { $('#resourceSelect').value = state.resourceSavedPath; return; }
    clearResource(); if (!path) return;
    const file = await request(endpoint() + '/file?path=' + encodeURIComponent(path)); state.resourceRevision = file.revision; state.resourceSavedPath = path;
    $('#resourcePath').value = path; $('#resourcePath').readOnly = true; $('#resourceText').value = file.text; $('#attachResource').disabled = false;
  });
  for (const selector of ['#resourcePath', '#resourceText']) $(selector).oninput = () => { state.resourceDirty = true; $('#attachResource').disabled = true; $('#resourceStatus').textContent = '檔案尚未保存'; };
  $('#saveResource').onclick = guarded(async () => {
    const path = $('#resourcePath').value.trim(); const file = await request(endpoint() + '/file', {method:'PUT', body:JSON.stringify({path, revision:state.resourceRevision, text:$('#resourceText').value})});
    state.resourceRevision = file.revision; state.resourceSavedPath = path; state.resourceDirty = false; $('#resourcePath').readOnly = true; $('#attachResource').disabled = false;
    await refreshResources(); $('#resourceSelect').value = path; $('#resourceStatus').textContent = '檔案已保存。已引用此檔案的 Skill 下次讀取會取得新版內容；新檔案請加入步驟。';
  });
  $('#attachResource').onclick = () => {
    if (!state.selected) { message('請先選擇一個步驟。', true); return; }
    const path = '.promptforge/resources/' + state.name + '/' + state.resourceSavedPath;
    const type = state.resourceSavedPath.startsWith('scripts/') ? 'script' : state.resourceSavedPath.startsWith('assets/') ? 'asset' : 'reference';
    state.selected.capabilities ||= []; if (!state.selected.capabilities.some(cap => cap.path === path)) state.selected.capabilities.push({type,path,selected:true});
    markDirty(); renderEditor(); message('已加入目前步驟；請填使用方法，再保存並生成 Skill。'); $('#editor').scrollIntoView();
  };
  for (const [button, source] of [['#copyUrl','#mcpUrl'],['#copyCommand','#mcpCommand'],['#copyPrompt','#agentPrompt']]) $(button).onclick = guarded(async () => { await navigator.clipboard.writeText($(source).textContent); message('已複製。'); });
  refreshSkills().then(load).catch(error => message(error.message, true));
})();
