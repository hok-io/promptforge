(() => {
  'use strict';
  const $ = s => document.querySelector(s);
  let model = null, receipt = null, poll = null, csrf = null;
  const routeAliases = {outline:'manage',resources:'manage',output:'manage',library:'manage'};
  function route() {
    const target = location.hash.slice(1) || 'home'; if (target === 'main') { $('#main').focus(); return; }
    const screen = routeAliases[target] || (['home','agent','verify','manage'].includes(target) ? target : 'home');
    document.querySelectorAll('[data-screen]').forEach(el => { el.hidden = el.dataset.screen !== screen; });
    document.querySelectorAll('[data-route]').forEach(el => { if (el.dataset.route === screen) el.setAttribute('aria-current','page'); else el.removeAttribute('aria-current'); });
    document.title = 'PromptForge｜' + ({home:'開始使用',agent:'連接 AI',verify:'驗證連接',manage:'管理 Skill'})[screen];
    if (['resources','output'].includes(target)) $('#' + target).open = true;
    const heading = document.querySelector('[data-screen="' + screen + '"] h1'); heading.tabIndex = -1; heading.focus({preventScroll:true});
    if (routeAliases[target]) $('#' + target).scrollIntoView(); else window.scrollTo(0,0);
    if (screen === 'verify' && receipt) refreshReceipt();
  }
  async function api(path, options = {}) {
    const headers = {'Content-Type':'application/json'};
    if (options.method) {
      if (!csrf) csrf = await (await fetch('/api/csrf')).json();
      headers[csrf.headerName] = csrf.token;
    }
    const response = await fetch(path, {...options,headers,signal:AbortSignal.timeout(12000)});
    const data = await response.json();
    if (!response.ok) throw Error(data.message || '請求失敗，請重試。');
    return data;
  }
  function selectedSteps() { return (model?.saved?.outline?.steps || []).filter(step => step.selected); }
  function command(task, name) {
    return '請透過 promptforge MCP 的 get_skill 讀取 name="' + name + '" 的最新保存版，然後依該 Skill 處理以下輸入。若無法讀取，請說明原因。\n\n' + task;
  }
  function updateTask() { $('#taskCommand').textContent = command($('#taskInput').value.trim() || '［在此填寫你的任務］',model?.name || 'PromptForge'); }
  async function copy(text, feedback) {
    try { await navigator.clipboard.writeText(text); feedback.textContent = '已複製。現在貼到你的 AI 對話。'; }
    catch { feedback.textContent = '瀏覽器未允許複製，請展開內容後手動複製。'; }
  }
  function updateModel(event) {
    const next = event.detail;
    if (model && model.name !== next.name) { resetReceipt(); $('#taskFeedback').textContent = ''; }
    model = next;
    $('#skillPurpose').textContent = next.saved?.outline?.description || (next.name === 'PromptForge' ? '把原始任務整理成可交給 AI 執行的 prompt，保留目標、限制、驗收與 ReAct 流程。' : '保存用途與步驟後，便可把這個 Skill 交給 AI 使用。');
    const list = $('#activeSteps'); list.replaceChildren();
    for (const step of selectedSteps()) { const li = document.createElement('li'); li.textContent = step.title; list.append(li); }
    if (!list.childElementCount) { const li = document.createElement('li'); li.textContent = '尚未生成。先到「管理 Skill」加入步驟並保存。'; list.append(li); }
    const ready = !!next.saved?.generatedCurrent;
    $('#homeReady').textContent = next.dirty ? '有未保存修改' : ready ? '保存版可使用' : '請先保存生成';
    $('#copyTask').disabled = !ready; $('#startVerification').disabled = !ready || next.dirty;
    $('#taskFeedback').textContent = next.dirty ? 'AI 只會讀取上次保存版。請先到「管理 Skill」保存修改。' : !ready ? '請先到「管理 Skill」保存並生成，再複製使用指令。' : '';
    $('#verificationError').textContent = !ready || next.dirty ? '請先到「管理 Skill」保存並生成，再開始驗證。' : '';
    $('#contextUsage').hidden = next.name !== 'PromptForge';
    const checks = next.name === 'PromptForge' ? [
      'Prompt 保留你的目標、範圍、限制、預期結果及驗收方法。',
      '包含判斷下一步 → 行動 → 觀察 → 調整／重試 → 完成或受阻的 ReAct 流程。',
      '把完整 prompt 交給 PromptForge-context，結果分清已讀資料、可讀但未讀、缺失資料及可完成／受阻範圍。',
      'Context 檢查沒有自動執行原任務；聲稱已取得的資料有實際讀取證據。'
    ] : selectedSteps().map(step => step.title + '：' + (step.output || '按步驟定義檢查實際輸出') + '；停止條件：' + (step.stop || '尚未填寫，請到管理 Skill 補充'));
    $('#acceptanceIntro').textContent = next.name === 'PromptForge' ? '先用一個小任務試跑，再核對以下結果。' : '依目前保存的步驟檢查 AI 輸出；完成條件由你在 Skill 內定義。';
    $('#acceptanceChecks').replaceChildren(...checks.map(text => { const li = document.createElement('li'); li.textContent = text; return li; }));
    updateTask(); if (receipt) refreshReceipt();
  }
  function resetReceipt() { clearTimeout(poll); poll = null; receipt = null; $('#verificationBody').hidden = true; $('#verificationState').textContent = '尚未開始'; }
  function renderReceipt(data) {
    receipt = data;
    const info = {
      waiting:['等候 AI 讀取','尚未收到回執','將上面的測試句貼到 AI，等它實際呼叫 get_skill。'],
      verified:['讀取驗證通過','MCP 已讀取指定版本','收到帶驗證碼的讀取呼叫：' + (data.readAt ? new Date(data.readAt).toLocaleString('zh-HK') : '') + '。可以返回「開始使用」交付任務。'],
      stale:['版本已變更','請驗證最新版本','Skill 已更新。按「產生測試句」取得新的驗證碼。'],
      expired:['驗證已過期','請重新產生測試句','這次驗證超過 15 分鐘，請重新開始。']
    }[data.state];
    $('#verificationState').textContent = info[0]; $('#receiptTitle').textContent = info[1]; $('#receiptDetail').textContent = info[2];
    $('#receiptVersion').textContent = data.skill + ' · 版本 ' + data.revision.slice(0,12);
  }
  async function refreshReceipt() {
    clearTimeout(poll); if (!receipt) return;
    const code = receipt.code;
    try { const data = await api('/api/verification/' + encodeURIComponent(code)); if (receipt?.code === code) { renderReceipt(data); $('#verificationError').textContent = ''; } }
    catch(error) { $('#verificationError').textContent = error.message + ' 可按「更新驗證結果」重試，或重新產生測試句。'; }
    if (receipt?.code === code && receipt.state === 'waiting' && !$('#verify').hidden) poll = setTimeout(refreshReceipt,3000);
  }
  $('#startVerification').onclick = async () => {
    const button = $('#startVerification'); button.disabled = true; button.textContent = '正在產生…';
    try {
      const data = await api('/api/verification',{method:'POST',body:JSON.stringify({skill:model.name})});
      resetReceipt(); renderReceipt(data); $('#verificationBody').hidden = false; $('#verificationError').textContent = '';
      $('#agentPrompt').value = '連接驗證：請實際呼叫 promptforge MCP 的 get_skill，傳入 name="' + data.skill + '" 及 verificationCode="' + data.code + '"。回報讀到的 Skill 名稱與步驟。這次只驗證讀取，先不要執行 Skill 的任務。';
      $('#copyPrompt').focus(); poll = setTimeout(refreshReceipt,3000);
    } catch(error) { $('#verificationError').textContent = error.message; }
    finally { button.disabled = !model?.saved?.generatedCurrent || model?.dirty; button.textContent = '產生測試句'; }
  };
  $('#refreshVerification').onclick = refreshReceipt;
  $('#copyPrompt').onclick = () => copy($('#agentPrompt').value, $('#verificationCopyFeedback'));
  $('#taskInput').oninput = updateTask;
  $('#copyTask').onclick = () => {
    if (!$('#taskInput').value.trim()) { $('#taskFeedback').textContent = '請先寫低你的任務，再複製使用指令。'; $('#taskInput').focus(); return; }
    copy($('#taskCommand').textContent,$('#taskFeedback'));
  };
  $('#copyContext').onclick = () => {
    if (!$('#contextInput').value.trim()) { $('#contextFeedback').textContent = '請先貼上完整 prompt。'; $('#contextInput').focus(); return; }
    copy(command($('#contextInput').value.trim(),'PromptForge-context'),$('#contextFeedback'));
  };
  document.addEventListener('program:change',updateModel);
  window.addEventListener('hashchange',route);
  document.addEventListener('visibilitychange',() => { if (document.hidden) clearTimeout(poll); else if (!$('#verify').hidden && receipt) refreshReceipt(); });
  route();
})();
