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
    document.title = 'PromptForge｜' + ({home:'Skill 庫',agent:'連接 AI',verify:'驗證連接',manage:'管理 Skill'})[screen];
    if (routeAliases[target]) document.dispatchEvent(new CustomEvent('manage:section',{detail:target === 'output' ? 3 : target === 'library' ? 1 : 2}));
    if (['resources','output'].includes(target)) $('#' + target).open = true;
    const heading = document.querySelector('[data-screen="' + screen + '"] h1'); heading.tabIndex = -1; heading.focus({preventScroll:true});
    if (routeAliases[target]) $('#' + target).scrollIntoView(); else window.scrollTo(0,0);
    $('#skillBar').hidden = screen === 'agent' || screen === 'home';
    if (screen === 'home' || screen === 'agent') document.dispatchEvent(new Event('library:refresh'));
    if (screen === 'verify' && receipt) refreshReceipt();
  }
  async function api(path, options = {}, retried = false) {
    const headers = {'Content-Type':'application/json'};
    if (options.method) {
      csrf = await (await fetch('/api/csrf', {cache:'no-store'})).json();
      headers[csrf.headerName] = csrf.token;
    }
    const response = await fetch(path, {...options,headers,signal:AbortSignal.timeout(12000)});
    const data = await response.json();
    if (response.status === 403 && data.code === 'CSRF_EXPIRED' && !retried) return api(path, options, true);
    if (!response.ok) throw Error(data.message || '請求失敗，請重試。');
    return data;
  }
  function selectedSteps() { return (model?.saved?.outline?.steps || []).filter(step => step.selected); }
  function command(task, name) {
    return '請透過 promptforge MCP 的 get_skill 讀取 name="' + name + '" 的最新保存版，然後依該 Skill 處理以下輸入。若無法讀取，請說明原因。\n\n' + task;
  }
  function updateTask() { $('#taskCommand').textContent = command($('#taskInput').value.trim() || '［在此填寫你的任務］',model?.name || ''); }
  async function copy(text, feedback) {
    try { await navigator.clipboard.writeText(text); feedback.textContent = '已複製。現在貼到你的 AI 對話。'; }
    catch { feedback.textContent = '瀏覽器未允許複製，請展開內容後手動複製。'; }
  }
  function updateModel(event) {
    const next = event.detail;
    if (model && model.name !== next.name) { resetReceipt(); $('#taskFeedback').textContent = ''; }
    model = next;
    $('#verifyTitle').textContent = '測試 ' + next.name;
    $('#verifySkillPurpose').textContent = next.saved?.outline?.description || '';

    $('#savedSkillContent').textContent = next.saved?.generated || '尚未生成；請先保存。';
    $('#skillInputHelp').textContent = next.saved?.outline?.input || '按這個 Skill 的指示，填入任務或所需資料。';
    $('#skillPurpose').textContent = next.saved?.outline?.description || '先填寫這個 Skill 的用途與指示，再保存。';
    const list = $('#activeSteps'); list.replaceChildren();
    for (const step of selectedSteps()) { const li = document.createElement('li'); li.textContent = step.title; list.append(li); }
    if (next.saved?.outline?.document) { for (const heading of [...next.saved.outline.document.matchAll(/^#{1,3} (.+)$/gm)].slice(0,6)) { const li=document.createElement('li'); li.textContent=heading[1]; list.append(li); } }
    if (!list.childElementCount) { const li = document.createElement('li'); li.textContent = next.saved?.outline?.document ? '完整指示可在「編輯」查看。' : '尚未生成。先到「管理 Skill」加入步驟並保存。'; list.append(li); }
    const ready = !!next.saved?.generatedCurrent;
    $('#homeReady').textContent = next.dirty ? '有未保存修改' : ready ? '保存版可使用' : '請先保存生成';
    $('#copyTask').disabled = !ready; $('#startVerification').disabled = !ready || next.dirty;
    $('#taskFeedback').textContent = next.dirty ? 'AI 只會讀取上次保存版。請先到「管理 Skill」保存修改。' : !ready ? '請先到「管理 Skill」保存並生成，再複製使用指令。' : '';
    $('#verificationError').textContent = !ready || next.dirty ? '請先到「管理 Skill」保存並生成，再開始驗證。' : '';
    const checks = selectedSteps().flatMap(step => [step.output, step.stop].filter(value => typeof value === 'string' && value.trim()).map(value => step.title + '：' + value));
    if (next.saved?.outline?.result) checks.unshift(next.saved.outline.result);
    if (!checks.length && next.saved?.outline?.document) {
      const sections = next.saved.outline.document.split(/(?=^#{1,4}\s)/m);
      for (const section of sections) {
        const lines = section.split(/\r?\n/), heading = (lines.shift() || '').replace(/^#{1,4}\s+/, '').trim();
        if (/(output|result|acceptance|completion|done|完成|輸出|驗收)/i.test(heading)) {
          const content = lines.join('\n').trim(); if (content) checks.push(heading + '：' + content);
        }
      }
    }
    $('#acceptanceIntro').textContent = checks.length ? '以下內容來自目前保存的 Skill；請據此核對實際任務結果。' : '目前 Skill 沒有獨立列出輸出或停止條件；請按管理內容中的指示核對結果。';
    $('#acceptanceChecks').replaceChildren(...checks.map(text => { const li = document.createElement('li'); li.textContent = text; return li; }));
    updateTask(); if (receipt) refreshReceipt();
  }
  function resetReceipt() { clearTimeout(poll); poll = null; receipt = null; $('#verificationBody').hidden = true; $('#verificationState').textContent = '尚未開始'; }
  function renderReceipt(data) {
    receipt = data;
    const info = {
      waiting:['等候 AI 讀取','AI 尚未讀取 Skill','將上面的測試指令貼到 AI，等它實際呼叫 get_skill。'],
      verified:['讀取驗證通過','AI 已成功讀取 Skill','讀取時間：' + (data.readAt ? new Date(data.readAt).toLocaleString('zh-HK') : '') + '。現在可以到「Skill 庫」使用這個 Skill。'],
      stale:['版本已變更','請驗證最新版本','Skill 已更新。按「產生測試指令」取得新的驗證碼。'],
      expired:['驗證已過期','請重新產生測試指令','這次驗證超過 15 分鐘，請重新開始。']
    }[data.state];
    $('#verificationState').textContent = info[0]; $('#receiptTitle').textContent = info[1]; $('#receiptDetail').textContent = info[2];
    $('#receiptVersion').textContent = data.skill + ' · 版本 ' + data.revision.slice(0,12);
  }
  async function refreshReceipt() {
    clearTimeout(poll); if (!receipt) return;
    const code = receipt.code;
    try { const data = await api('/api/verification/' + encodeURIComponent(code)); if (receipt?.code === code) { renderReceipt(data); $('#verificationError').textContent = ''; } }
    catch(error) { $('#verificationError').textContent = error.message + ' 可按「更新驗證結果」重試，或重新產生測試指令。'; }
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
    finally { button.disabled = !model?.saved?.generatedCurrent || model?.dirty; button.textContent = '產生測試指令'; }
  };
  $('#copyLibraryRead').onclick = () => copy($('#libraryReadCommand').textContent, $('#connectionCopyFeedback'));
  $('#refreshVerification').onclick = refreshReceipt;
  $('#copyPrompt').onclick = () => copy($('#agentPrompt').value, $('#verificationCopyFeedback'));
  $('#taskInput').oninput = updateTask;
  $('#copyTask').onclick = () => {
    if (!$('#taskInput').value.trim()) { $('#taskFeedback').textContent = '請先輸入任務，再複製指令。'; $('#taskInput').focus(); return; }
    copy($('#taskCommand').textContent,$('#taskFeedback'));
  };
  document.addEventListener('program:change',updateModel);
  window.addEventListener('hashchange',route);
  document.addEventListener('visibilitychange',() => { if (document.hidden) clearTimeout(poll); else if (!$('#verify').hidden && receipt) refreshReceipt(); });
  route();
})();
