(() => {
  'use strict';
  const $ = (selector, root = document) => root.querySelector(selector);
  const state = { projectId: '', projects: [], context: null, csrf: null, generation: 0, previewSequence: 0, knowledgeSearchSequence: 0, document: null, revision: null, activePromptId: '', excludedRuleIds: [], skill: null, skillRevision: null, skillDetailTab: 'references', dirtyPrompt: false, dirtySkill: false, pendingSave: null };
  const els = {
    project: $('#projectSelect'), promptSelect: $('#promptSelect'), name: $('#promptName'), promptId: $('#promptId'), task: $('#taskInput'), blocks: $('#blocksList'), vars: $('#variablesInput'), rules: $('#rulesList'), knowledge: $('#knowledgeList'), preview: $('#previewOutput'), meta: $('#previewMeta'), skills: $('#skillsList'), toast: $('#toast'), diff: $('#diffDialog'), import: $('#importDialog'), export: $('#exportDialog')
  };
  function toast(message, error = false) { els.toast.textContent = message; els.toast.classList.toggle('error', error); els.toast.classList.add('show'); clearTimeout(toast.timer); toast.timer = setTimeout(() => els.toast.classList.remove('show'), 4200); }
  async function request(path, options = {}, generation = state.generation) {
    const headers = new Headers(options.headers || {});
    if (options.body !== undefined && !(options.body instanceof FormData)) headers.set('Content-Type', 'application/json');
    if (options.method && !['GET', 'HEAD'].includes(options.method.toUpperCase())) {
      if (!state.csrf) await loadCsrf();
      headers.set(state.csrf.headerName, state.csrf.token);
    }
    let response;
    try { response = await fetch(path, { ...options, headers }); }
    catch (_) { throw new Error('無法連線至本機工作台，請確認服務已啟動。'); }
    let data = null;
    try { data = await response.json(); } catch (_) { /* response may have no body */ }
    if (generation !== state.generation) throw new Error('專案已切換，已忽略較早的回應。');
    if (!response.ok) {
      const message = data && typeof data.message === 'string' ? data.message : `要求失敗（${response.status}）`;
      const error = new Error(`${message}（${response.status}）`); error.status = response.status; throw error;
    }
    return data;
  }
  async function loadCsrf() {
    const response = await fetch('/api/csrf');
    if (!response.ok) throw new Error('無法取得本機安全連線資訊。');
    state.csrf = await response.json();
  }
  function setBusy(button, busy, busyText = '處理中…') { if (!button) return; if (busy) { button.dataset.label = button.textContent; button.textContent = busyText; button.disabled = true; } else { button.textContent = button.dataset.label || button.textContent; button.disabled = false; } }
  function node(tag, className, text) { const el = document.createElement(tag); if (className) el.className = className; if (text !== undefined) el.textContent = text; return el; }
  function escAttr(value) { return String(value ?? ''); }
  function currentPath(suffix) { return `/api/projects/${encodeURIComponent(state.projectId)}${suffix}`; }
  function markPromptDirty() { state.dirtyPrompt = true; }
  function refreshModeHint() { const template = $('#modeSelect').value === 'template'; $('#modeHint').textContent = template ? '自訂段落會按下方次序加入結果。' : '預覽會加入你核對過的規則與知識；需要固定格式時再加入自訂段落。'; $('#blocksCard').hidden = !template; }
  function makeBlock(name = '指示', text = '') {
    const row = node('div', 'block-row'); row.dataset.block = 'true';
    const top = node('div', 'block-top'); const label = node('label', 'block-name-label', '區塊名稱'); const nameInput = node('input', 'block-name'); nameInput.value = name; nameInput.maxLength = 80; label.append(nameInput);
    const actions = node('div', 'block-actions');
    [['↑', '上移區塊', -1], ['↓', '下移區塊', 1], ['×', '刪除區塊', 0]].forEach(([caption, title, delta]) => { const button = node('button', 'icon-button block-move', caption); button.type = 'button'; button.title = title; button.setAttribute('aria-label', title); button.addEventListener('click', () => { if (delta === 0) row.remove(); else { const sibling = delta < 0 ? row.previousElementSibling : row.nextElementSibling; if (sibling) row.parentElement.insertBefore(row, delta < 0 ? sibling : sibling.nextElementSibling); } markPromptDirty(); }); actions.append(button); });
    top.append(label, actions); const contentLabel = node('label', 'field block-content'); contentLabel.append(node('span', '', '內容')); const area = node('textarea'); area.rows = 3; area.placeholder = '寫下明確、可執行的指示'; area.value = text; contentLabel.append(area); row.append(top, contentLabel);
    row.addEventListener('input', markPromptDirty); return row;
  }
  function collectDocument() {
    let variables; try { variables = JSON.parse(els.vars.value || '{}'); } catch (_) { throw new Error('變數必須是有效的 JSON。'); }
    if (!variables || Array.isArray(variables) || typeof variables !== 'object') throw new Error('變數必須是 JSON 物件。');
    const selectedMode = document.querySelector('input[name="ruleMode"]:checked')?.value || 'auto';
    const selectedRules = [...els.rules.querySelectorAll('input[type=checkbox]:checked')].map(input => input.value);
    const id = state.activePromptId || els.promptId.value.trim() || 'draft';
    return { id, name: els.name.value.trim() || '未命名提示詞', task: els.task.value.trim(), blocks: [...els.blocks.querySelectorAll('[data-block]')].map(row => ({ name: $('.block-name', row).value.trim(), text: $('textarea', row).value })), variables, ruleIds: selectedMode === 'auto' ? null : selectedRules, excludedRuleIds: state.excludedRuleIds, knowledgeSources: [...els.knowledge.querySelectorAll('input:checked')].map(input => input.value), mode: $('#modeSelect').value };
  }
  function renderSelection(context) {
    els.rules.replaceChildren(); const ruleMode = node('div', 'rule-mode');
    [['auto', '自動選擇規則'], ['manual', '手動選擇']].forEach(([value, label]) => { const wrap = node('label', 'radio-choice'); const input = document.createElement('input'); input.type = 'radio'; input.name = 'ruleMode'; input.value = value; input.checked = value === 'auto'; wrap.append(input, document.createTextNode(label)); ruleMode.append(wrap); }); els.rules.append(ruleMode);
    const ruleRows = node('div', 'rule-options');
    (context.rules || []).forEach(rule => { const label = node('label', 'check-row'); const input = document.createElement('input'); input.type = 'checkbox'; input.value = escAttr(rule.id); input.disabled = true; input.addEventListener('change', markPromptDirty); const copy = node('span', 'check-copy'); copy.append(node('strong', '', rule.id || '規則'), node('small', '', rule.instruction || '')); label.append(input, copy); ruleRows.append(label); });
    els.rules.append(ruleRows); els.rules.querySelectorAll('input[name=ruleMode]').forEach(input => input.addEventListener('change', () => { const manual = input.checked && input.value === 'manual'; if (manual) { els.rules.querySelectorAll('.rule-options input').forEach(box => { box.disabled = false; }); } else if (input.checked) { els.rules.querySelectorAll('.rule-options input').forEach(box => { box.disabled = true; box.checked = false; }); } markPromptDirty(); })); $('#ruleCount').textContent = String((context.rules || []).length);
    els.knowledge.replaceChildren(); (context.knowledge || []).forEach(card => { const label = node('label', 'knowledge-card'); const input = document.createElement('input'); input.type = 'checkbox'; input.value = card.source || card.title; input.addEventListener('change', () => { const chosen = [...els.knowledge.querySelectorAll('input:checked')]; if (chosen.length > 3) { input.checked = false; toast('知識卡片最多選取 3 張。', true); } $('#knowledgeCount').textContent = `${els.knowledge.querySelectorAll('input:checked').length} / 3`; markPromptDirty(); }); const body = node('span', 'knowledge-copy'); body.append(node('strong', '', card.title || '知識卡片'), node('small', '', card.body || '')); label.append(input, body); els.knowledge.append(label); }); if (!context.knowledge?.length) els.knowledge.append(node('span', 'muted small', '目前專案沒有可用的知識卡片。')); $('#knowledgeCount').textContent = `0 / 3`;
  }
  async function searchKnowledge() {
    const query = $('#knowledgeQuery').value.trim(); const sequence = ++state.knowledgeSearchSequence; const generation = state.generation; const results = $('#knowledgeSearchResults');
    if (!query) { results.replaceChildren(node('span', 'muted small', '輸入關鍵字以搜尋此專案的知識。')); return; }
    results.replaceChildren(node('span', 'muted small', '搜尋中…'));
    try {
      const hits = await request(currentPath(`/knowledge/search?q=${encodeURIComponent(query)}`), {}, generation);
      if (generation !== state.generation || sequence !== state.knowledgeSearchSequence) return;
      results.replaceChildren();
      (hits || []).forEach(hit => {
        const button = node('button', 'knowledge-hit'); button.type = 'button';
        button.append(node('strong', '', hit.title || '知識卡片'), node('small', 'knowledge-hit-source', hit.source || ''), node('span', 'knowledge-hit-excerpt', hit.excerpt || ''));
        button.addEventListener('click', () => { const checkbox = [...els.knowledge.querySelectorAll('input[type=checkbox]')].find(input => input.value === hit.source); if (!checkbox) { toast('搜尋結果目前不在卡片清單中。', true); return; } if (!checkbox.checked) checkbox.click(); }); results.append(button);
      });
      if (!(hits || []).length) results.append(node('span', 'muted small', '沒有符合的知識卡片。'));
    } catch (error) { if (generation === state.generation && sequence === state.knowledgeSearchSequence) { results.replaceChildren(node('span', 'form-error', error.message)); toast(error.message, true); } }
  }
  function renderPromptList(prompts) { const previous = state.activePromptId; els.promptSelect.replaceChildren(); const blank = node('option', '', '建立新草稿'); blank.value = ''; els.promptSelect.append(blank); (prompts || []).forEach(item => { const option = node('option', '', `${item.name} · r${item.revision}`); option.value = item.id; els.promptSelect.append(option); }); if (previous && [...els.promptSelect.options].some(o => o.value === previous)) els.promptSelect.value = previous; }
  function renderSkills(skills) { els.skills.replaceChildren(); (skills || []).forEach(skill => { const button = node('button', `skill-row${state.skill?.id === skill.id ? ' selected' : ''}`); button.type = 'button'; button.append(node('strong', '', skill.name), node('small', '', skill.description || '無摘要'), node('span', 'badge', skill.state || '草稿')); button.addEventListener('click', () => openSkill(skill.id)); els.skills.append(button); }); if (!skills?.length) els.skills.append(node('span', 'muted small', '此專案尚無技能。'));
  }
  function updateSkillSummary(detail) { const summary = state.context?.skills?.find(item => item.id === detail.id); if (summary) Object.assign(summary, { name: detail.name, description: detail.description, revision: detail.revision, state: detail.state, origin: detail.origin }); renderSkills(state.context?.skills || []); }
  function skillActionLabel(skillState) { return ({ IMPORTED: '送交審核', DRAFT: '送交審核', REVIEWED: '核准', REVIEW: '核准', APPROVED: '標記為目前版本', ACTIVE: '目前版本' })[skillState] || '送交審核'; }
  function updateSkillHeader(detail) { $('#skillName').textContent = detail.name; $('#skillDescription').textContent = detail.description || ''; $('#skillState').textContent = detail.state || '草稿'; $('#skillActionButton').textContent = skillActionLabel(detail.state); $('#skillActionButton').disabled = detail.state === 'ACTIVE'; }
  async function loadProject(id) {
    state.generation++; const generation = state.generation; state.projectId = id; state.context = null; state.skill = null; state.skillRevision = null; state.activePromptId = ''; state.revision = null; state.dirtyPrompt = false; state.dirtySkill = false;
    $('#projectName').textContent = els.project.selectedOptions[0]?.textContent || '選擇專案'; els.rules.textContent = '載入中…'; els.knowledge.textContent = '載入中…'; els.skills.textContent = '載入中…'; els.preview.textContent = '載入專案資料中…'; $('#skillEditor').hidden = true; $('#skillEmpty').hidden = false; els.promptId.value = ''; $('#promptIdField').hidden = false; els.name.value = ''; els.task.value = ''; els.vars.value = '{}'; $('#modeSelect').value = 'frame'; refreshModeHint(); els.blocks.replaceChildren(makeBlock('目標', ''), makeBlock('要求', '')); state.excludedRuleIds = [];
    if (!id) return;
    try { const context = await request(`/api/projects/${encodeURIComponent(id)}/context`, {}, generation); if (generation !== state.generation) return; state.context = context; renderSelection(context); renderPromptList(context.prompts); renderSkills(context.skills); $('#connectionState').textContent = '已連線至本機'; $('#connectionState').classList.add('online'); await updatePreview(generation); }
    catch (error) { if (generation === state.generation) { toast(error.message, true); $('#connectionState').textContent = '連線失敗'; $('#connectionState').classList.remove('online'); els.preview.textContent = error.message; } }
  }
  function promptSnapshot(doc) { return JSON.stringify(doc, null, 2); }
  async function updatePreview(generation = state.generation) {
    if (!state.projectId) { els.preview.textContent = '請先選擇專案。'; return; }
    const sequence = ++state.previewSequence;
    try { const doc = collectDocument(); if (!doc.task.trim() && !(doc.mode === 'template' && doc.blocks.some(block => block.text.trim()))) { els.meta.textContent = '等待任務內容'; els.preview.textContent = '輸入任務，或在提示詞範本加入區塊，再按更新預覽。'; return; } els.meta.textContent = '正在更新預覽…'; const result = await request(currentPath('/preview'), { method: 'POST', body: JSON.stringify(doc) }, generation); if (generation !== state.generation || sequence !== state.previewSequence) return; els.preview.textContent = result.text || ''; els.meta.textContent = `${doc.mode === 'frame' ? '任務框架' : '提示詞範本'} · ${doc.mode === 'template' ? doc.blocks.length : 0} 個區塊`; }
    catch (error) { if (generation === state.generation && sequence === state.previewSequence) { els.meta.textContent = '預覽未更新'; els.preview.textContent = error.message; if (error.status) toast(error.message, true); } }
  }
  function fillPrompt(documentData, revision, promptId) {
    state.document = documentData; state.revision = revision; state.activePromptId = promptId || ''; state.excludedRuleIds = [...(documentData.excludedRuleIds || [])]; els.promptSelect.value = promptId || ''; els.name.value = documentData.name || ''; els.task.value = documentData.task || ''; $('#modeSelect').value = documentData.mode || 'frame'; refreshModeHint(); els.vars.value = JSON.stringify(documentData.variables || {}, null, 2); els.blocks.replaceChildren(); (documentData.blocks || []).forEach(b => els.blocks.append(makeBlock(b.name, b.text)));
    els.promptId.value = promptId || documentData.id || ''; $('#promptIdField').hidden = Boolean(promptId);
    const manual = Array.isArray(documentData.ruleIds); const radio = document.querySelector(`input[name="ruleMode"][value="${manual ? 'manual' : 'auto'}"]`); if (radio) { radio.checked = true; els.rules.querySelectorAll('.rule-options input').forEach(box => { box.disabled = !manual; box.checked = manual && documentData.ruleIds.includes(box.value); }); }
    els.knowledge.querySelectorAll('input').forEach(box => { box.checked = (documentData.knowledgeSources || []).includes(box.value); }); $('#knowledgeCount').textContent = `${els.knowledge.querySelectorAll('input:checked').length} / 3`; state.dirtyPrompt = false; updatePreview();
  }
  async function loadPrompt(id) { if (!id) { state.revision = null; state.activePromptId = ''; state.excludedRuleIds = []; els.promptId.value = ''; $('#promptIdField').hidden = false; els.name.value = ''; els.task.value = ''; els.vars.value = '{}'; $('#modeSelect').value = 'frame'; refreshModeHint(); els.blocks.replaceChildren(makeBlock('目標', ''), makeBlock('要求', '')); const auto = document.querySelector('input[name="ruleMode"][value="auto"]'); if (auto) auto.checked = true; els.rules.querySelectorAll('.rule-options input').forEach(box => { box.checked = false; box.disabled = true; }); els.knowledge.querySelectorAll('input').forEach(box => { box.checked = false; }); $('#knowledgeCount').textContent = '0 / 3'; state.dirtyPrompt = false; updatePreview(); return; } try { const result = await request(currentPath(`/prompts/${encodeURIComponent(id)}`)); fillPrompt(result.document, result.revision, id); } catch (error) { toast(error.message, true); } }
  async function savePrompt(button) {
    const doc = collectDocument(); if (!doc.task && !(doc.mode === 'template' && doc.blocks.some(block => block.text.trim()))) throw new Error('請填寫任務，或在提示詞範本加入非空區塊。'); const id = state.activePromptId || els.promptId.value.trim(); if (!id) { $('#saveOptions').open = true; els.promptId.focus(); throw new Error('請在「載入或儲存常用提示詞」填寫新提示詞 ID。'); } if (!/^[a-z][a-z0-9-]{0,39}$/.test(id)) throw new Error('提示詞 ID 請以小寫英文字母開頭，只含小寫英數字與短橫線，最多 40 字。'); doc.id = id; const revision = state.revision || 'new';
    const diff = await request(currentPath(`/prompts/${encodeURIComponent(id)}/diff`), { method: 'POST', body: JSON.stringify({ revision, document: doc }) });
    $('#diffTitle').textContent = state.activePromptId ? '確認提示詞變更' : '確認新增提示詞'; $('#diffBefore').textContent = typeof diff.before === 'string' ? diff.before : JSON.stringify(diff.before ?? {}, null, 2); $('#diffAfter').textContent = typeof diff.after === 'string' ? diff.after : JSON.stringify(diff.after ?? doc, null, 2);
    state.pendingSave = async () => { const saved = await request(currentPath(`/prompts/${encodeURIComponent(id)}`), { method: 'PUT', body: JSON.stringify({ revision, document: doc }) }); const envelope = saved.prompt || saved; const savedDoc = envelope.document || doc; const savedId = envelope.id || saved.id || id; state.activePromptId = savedId; state.revision = envelope.revision || saved.revision; state.dirtyPrompt = false; if (state.context) { const item = { id: state.activePromptId, name: savedDoc.name || doc.name, revision: state.revision }; state.context.prompts = [...(state.context.prompts || []).filter(p => p.id !== item.id), item]; renderPromptList(state.context.prompts); } fillPrompt(savedDoc, state.revision, state.activePromptId); toast('提示詞已儲存。'); };
    els.diff.showModal(); $('#confirmSaveButton').onclick = async event => { const button = event.currentTarget; event.preventDefault(); setBusy(button, true, '儲存中…'); try { await state.pendingSave(); els.diff.close(); } catch (error) { toast(error.status === 409 ? `版本衝突：${error.message} 原提示詞不會被覆蓋，請使用其他 ID 或重新載入。` : error.message, true); } finally { setBusy(button, false); } };
  }
  async function openSkill(id) { const generation = state.generation; try { const detail = await request(currentPath(`/skills/${encodeURIComponent(id)}`), {}, generation); if (generation !== state.generation) return; state.skill = detail; state.skillRevision = detail.revision; state.dirtySkill = false; $('#skillEmpty').hidden = true; $('#skillEditor').hidden = false; updateSkillHeader(detail); $('#skillRaw').value = detail.raw || ''; $('#evaluationInput').value = JSON.stringify(evaluationExample(), null, 2); $('#evaluationError').textContent = ''; renderSkills(state.context?.skills || []); renderSkillSupplement(); }
    catch (error) { toast(error.message, true); } }
  function evaluationExample() {
    const stamp = new Date().toISOString();
    return { id: `eval-${stamp.replace(/[-:.TZ]/g, '').slice(0, 14)}`, subjectHash: state.skillRevision, kind: 'TEST', model: null, evaluator: '人工', timestamp: stamp, rubricVersion: '1', findings: ['請填寫一項評估發現'], evidence: [{ path: '請改成專案內既存檔案的相對路徑', status: 'PASS' }] };
  }
  async function submitEvaluation(button) {
    if (state.dirtySkill) throw new Error('先儲存技能編輯，或重新載入最新版本，再新增評估。');
    const errorBox = $('#evaluationError'); errorBox.textContent = '';
    let artifact; try { artifact = JSON.parse($('#evaluationInput').value); } catch (_) { throw new Error('評估資料不是有效的 JSON。'); }
    if (!artifact || Array.isArray(artifact) || typeof artifact !== 'object') throw new Error('評估資料必須是 JSON 物件。');
    if (!['TEST', 'MODEL_REVIEW'].includes(artifact.kind)) throw new Error('kind 必須是 TEST 或 MODEL_REVIEW。');
    if (!Array.isArray(artifact.findings) || !artifact.findings.every(value => typeof value === 'string')) throw new Error('findings 必須是文字陣列。');
    if (!Array.isArray(artifact.evidence) || !artifact.evidence.every(item => item && typeof item.path === 'string' && item.path.trim() && ['PASS', 'FAIL', 'SKIP'].includes(item.status))) throw new Error('evidence 必須包含既存檔案相對路徑與 PASS、FAIL 或 SKIP 狀態。');
    if (typeof artifact.subjectHash !== 'string' || !/^[a-fA-F0-9]{64}$/.test(artifact.subjectHash)) throw new Error('subjectHash 必須是 64 位十六進位版本雜湊；保留舊雜湊可記錄為過期評估。');
    if (!artifact.id || !artifact.evaluator || !artifact.timestamp || !artifact.rubricVersion) throw new Error('請提供 id、evaluator、timestamp（ISO-8601）與 rubricVersion。');
    const detail = await request(currentPath(`/skills/${encodeURIComponent(state.skill.id)}/evaluations`), { method: 'POST', body: JSON.stringify(artifact) });
    await openSkill(state.skill.id); $('#evaluationInput').value = JSON.stringify(evaluationExample(), null, 2); toast(`評估記錄已新增${detail?.id ? `（${detail.id}）` : ''}。`);
  }
  function renderSkillSupplement() { const target = $('#skillSupplement'); target.replaceChildren(); const tab = state.skillDetailTab; const rows = tab === 'references' ? (state.skill?.references || []) : (state.skill?.evaluations || []); if (!rows.length) { target.append(node('span', 'muted small', tab === 'references' ? '沒有參考檔案。' : '尚無評估記錄。')); return; } rows.forEach(item => { const card = node('article', 'supplement-card'); if (tab === 'references') { card.append(node('strong', '', item.path), node('pre', '', item.content || '')); } else { card.append(node('strong', '', `${item.kind || '評估'} · ${item.timestamp || ''}`), node('small', '', `${item.model || '本機'} · ${item.stale ? '已過期' : '有效'} · ${item.subjectHash || ''}`)); (item.findings || []).forEach(f => card.append(node('p', '', f))); (item.evidence || []).forEach(ev => card.append(node('small', 'evidence', `${ev.status} · ${ev.path}`))); } target.append(card); }); }
  async function saveSkill() { const raw = $('#skillRaw').value; if (!state.skill) return; const diff = await request(currentPath(`/skills/${encodeURIComponent(state.skill.id)}/diff`), { method: 'POST', body: JSON.stringify({ revision: state.skillRevision, raw }) }); $('#diffTitle').textContent = '確認技能變更'; $('#diffBefore').textContent = diff.before || ''; $('#diffAfter').textContent = diff.after || raw; state.pendingSave = async () => { state.skill = await request(currentPath(`/skills/${encodeURIComponent(state.skill.id)}`), { method: 'PUT', body: JSON.stringify({ revision: state.skillRevision, raw }) }); state.skillRevision = state.skill.revision; state.dirtySkill = false; $('#skillRaw').value = state.skill.raw || raw; updateSkillHeader(state.skill); $('#evaluationInput').value = JSON.stringify(evaluationExample(), null, 2); updateSkillSummary(state.skill); toast('技能已儲存。'); }; els.diff.showModal(); $('#confirmSaveButton').onclick = async event => { const button = event.currentTarget; event.preventDefault(); try { await state.pendingSave(); els.diff.close(); } catch (error) { toast(error.status === 409 ? `版本衝突：${error.message} 請重新載入技能後再儲存。` : error.message, true); } }; }
  async function skillStateAction() { if (state.dirtySkill) throw new Error('先儲存技能編輯，或重新載入最新版本，再變更狀態。'); const action = ({ IMPORTED: 'review', DRAFT: 'review', REVIEWED: 'approve', REVIEW: 'approve', APPROVED: 'activate' })[state.skill?.state]; if (!action) throw new Error('此技能目前沒有可用的狀態操作。'); const title = action === 'review' ? '送交審核' : action === 'approve' ? '核准' : '標記為工作台目前版本（不會安裝到 Codex）'; if (action !== 'review' && !confirm(`確定${title}技能「${state.skill.name}」？`)) return; const detail = await request(currentPath(`/skills/${encodeURIComponent(state.skill.id)}/state`), { method: 'POST', body: JSON.stringify({ revision: state.skillRevision, action }) }); state.skill = detail; state.skillRevision = detail.revision; updateSkillHeader(detail); updateSkillSummary(detail); toast(`技能狀態已更新：${detail.state}`); }
  async function importSkill() { const source = $('#importSource').value.trim(); const id = $('#importId').value.trim() || source; if (!source) throw new Error('請輸入收件匣資料夾名稱。'); if (!/^[a-z][a-z0-9-]{0,39}$/.test(id)) throw new Error('技能 ID 請以小寫英文字母開頭，只含小寫英數字與短橫線，最多 40 字。'); if (!/^[a-z][a-z0-9-]{0,39}$/.test(source)) throw new Error('收件匣資料夾名稱請符合技能 ID 格式。'); const detail = await request(currentPath('/skills/import'), { method: 'POST', body: JSON.stringify({ id, source }) }); if (state.context) { state.context.skills = [...(state.context.skills || []).filter(s => s.id !== detail.id), detail]; renderSkills(state.context.skills); } els.import.close(); $('#importSource').value = ''; $('#importId').value = ''; await openSkill(detail.id); toast('技能已匯入。'); }
  async function exportSkill() { if (state.dirtySkill) throw new Error('先儲存技能編輯，或重新載入最新版本，再匯出。'); if (!['APPROVED', 'ACTIVE'].includes(state.skill?.state)) throw new Error('請先核准目前技能版本，再匯出。'); const target = $('#exportTarget').value.trim(); if (!target) throw new Error('請輸入匯出名稱。'); if (!/^[a-z][a-z0-9-]{0,39}$/.test(target)) throw new Error('匯出名稱請以小寫英文字母開頭，只含小寫英數字與短橫線，最多 40 字。'); if (!confirm(`將技能「${state.skill.name}」匯出為「${target}」至目前專案的 .promptforge/exports/${target}。繼續嗎？`)) return; const result = await request(currentPath(`/skills/${encodeURIComponent(state.skill.id)}/export`), { method: 'POST', body: JSON.stringify({ revision: state.skillRevision, target }) }); els.export.close(); $('#exportTarget').value = ''; toast(`技能已匯出至 ${result.path}`); }
  function hasDirty() { return state.dirtyPrompt || state.dirtySkill; }
  function bind() {
    els.project.addEventListener('change', async () => { if (hasDirty() && !confirm('目前有尚未儲存的變更。切換專案會捨棄這些變更，確定繼續嗎？')) { els.project.value = state.projectId; return; } await loadProject(els.project.value); });
    $('#refreshButton').addEventListener('click', async () => { if (hasDirty() && !confirm('重新載入會捨棄尚未儲存的變更，確定繼續嗎？')) return; await loadProject(state.projectId); });
    document.querySelectorAll('.side-tab').forEach(button => button.addEventListener('click', () => { const prompt = button.dataset.tab === 'prompt'; document.querySelectorAll('.side-tab').forEach(tab => tab.classList.toggle('active', tab === button)); $('#promptView').classList.toggle('active', prompt); $('#skillsView').classList.toggle('active', !prompt); $('#promptRulesSection').hidden = !prompt; $('#promptKnowledgeSection').hidden = !prompt; }));
    els.promptSelect.addEventListener('change', () => loadPrompt(els.promptSelect.value)); $('#newPromptButton').addEventListener('click', () => loadPrompt('')); $('#addBlockButton').addEventListener('click', () => { els.blocks.append(makeBlock('新區塊', '')); markPromptDirty(); });
    [els.name, els.promptId, els.task, els.vars, $('#modeSelect')].forEach(control => control.addEventListener('input', markPromptDirty)); $('#refreshPreviewButton').addEventListener('click', () => updatePreview()); $('#savePromptButton').addEventListener('click', async event => { const button = event.currentTarget; setBusy(button, true); try { await savePrompt(button); } catch (error) { toast(error.message, true); } finally { setBusy(button, false); } });
    $('#copyPreviewButton').addEventListener('click', async () => { try { await navigator.clipboard.writeText(els.preview.textContent); toast('預覽已複製。'); } catch (_) { toast('複製失敗，請手動選取預覽文字。', true); } });
    $('#searchKnowledgeButton').addEventListener('click', searchKnowledge); $('#knowledgeQuery').addEventListener('keydown', event => { if (event.key === 'Enter') { event.preventDefault(); searchKnowledge(); } });
    $('#importSkillButton').addEventListener('click', () => els.import.showModal()); $('#confirmImportButton').addEventListener('click', async event => { const button = event.currentTarget; event.preventDefault(); try { setBusy(button, true); await importSkill(); } catch (error) { toast(error.message, true); } finally { setBusy(button, false); } });
    $('#saveSkillButton').addEventListener('click', async () => { try { await saveSkill(); } catch (error) { toast(error.message, true); } }); $('#skillActionButton').addEventListener('click', async () => { try { await skillStateAction(); } catch (error) { toast(error.message, true); } }); $('#exportSkillButton').addEventListener('click', () => { if (state.skill) els.export.showModal(); }); $('#confirmExportButton').addEventListener('click', async event => { const button = event.currentTarget; event.preventDefault(); try { setBusy(button, true); await exportSkill(); } catch (error) { toast(error.status === 409 ? `匯出目標已存在：${error.message}` : error.message, true); } finally { setBusy(button, false); } });
    $('#submitEvaluationButton').addEventListener('click', async event => { const button = event.currentTarget; setBusy(button, true, '新增中…'); try { await submitEvaluation(button); } catch (error) { $('#evaluationError').textContent = error.message; toast(error.message, true); } finally { setBusy(button, false); } });
    $('#modeSelect').addEventListener('change', refreshModeHint); $('#skillRaw').addEventListener('input', () => { state.dirtySkill = true; }); document.querySelectorAll('.detail-tab').forEach(button => button.addEventListener('click', () => { state.skillDetailTab = button.dataset.detail; document.querySelectorAll('.detail-tab').forEach(tab => tab.classList.toggle('active', tab === button)); renderSkillSupplement(); }));
  }
  async function start() { bind(); try { await loadCsrf(); const projects = await request('/api/projects'); state.projects = projects || []; els.project.replaceChildren(); state.projects.forEach(project => { const option = node('option', '', project.name); option.value = project.id; els.project.append(option); }); if (!state.projects.length) { els.project.append(node('option', '', '沒有可用專案')); $('#connectionState').textContent = '沒有可用專案'; return; } await loadProject(state.projects[0].id); }
    catch (error) { $('#connectionState').textContent = '無法連線'; toast(error.message, true); els.project.replaceChildren(node('option', '', '無法載入專案')); }
  }
  start();
})();


