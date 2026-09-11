(() => {
  let revision = 'new', outline = { steps: [] }, csrf;
  const fields = [['id','ID'],['title','名稱'],['purpose','目的'],['instructions','指示'],['input','輸入'],['decision','判斷依據'],['actions','可採取行動'],['observation','預期觀察'],['adjustment','失敗後調整'],['output','輸出'],['stop','停止條件'],['scripts','Scripts（每行一個 repo 相對路徑）'],['references','參考資料（每行一個 repo 相對路徑）'],['skills','其他 Skill（每行一個 SKILL.md 路徑）']];
  const $ = s => document.querySelector(s);
  function message(s, error=false) { $('#status').textContent=s; $('#status').className=error?'error':''; }
  function render() {
    $('#steps').replaceChildren();
    outline.steps.forEach((step,index) => {
      const card=document.createElement('section'); card.className='step';
      const head=document.createElement('div'); head.className='stephead';
      const selected=document.createElement('label'); const check=document.createElement('input'); check.type='checkbox'; check.checked=!!step.selected; check.onchange=()=>step.selected=check.checked; selected.append(check,document.createTextNode(' 提供給 agent')); head.append(selected);
      for(const [label,delta] of [['↑',-1],['↓',1],['刪除',0]]) { const b=document.createElement('button');b.textContent=label;b.type='button';b.onclick=()=>{if(delta===0)outline.steps.splice(index,1);else if(outline.steps[index+delta]) [outline.steps[index],outline.steps[index+delta]]=[outline.steps[index+delta],outline.steps[index]];render();};head.append(b); }
      card.append(head); const grid=document.createElement('div');grid.className='grid';
      for(const [key,title] of fields) { const label=document.createElement('label');label.textContent=title;const input=['id','title'].includes(key)?document.createElement('input'):document.createElement('textarea');if(input.tagName==='INPUT')input.type='text';input.value=Array.isArray(step[key])?step[key].join('\n'):(step[key]||'');input.oninput=()=>step[key]=['scripts','references','skills'].includes(key)?input.value.split('\n').map(x=>x.trim()).filter(Boolean):input.value;label.append(input);grid.append(label); }card.append(grid);
      const heading=document.createElement('h3');heading.textContent='可選能力';card.append(heading);const list=document.createElement('div');card.append(list);step.capabilities ||= [];
      step.capabilities.forEach((cap,i)=>{const row=document.createElement('div');row.className='capability';const selected=document.createElement('input');selected.type='checkbox';selected.checked=!!cap.selected;selected.onchange=()=>cap.selected=selected.checked;const type=document.createElement('select');for(const value of ['script','reference','skill']){const option=document.createElement('option');option.value=value;option.textContent=value;type.append(option);}type.value=cap.type||'reference';type.onchange=()=>cap.type=type.value;const path=document.createElement('input');path.type='text';path.placeholder='repo 相對檔案路徑';path.value=cap.path||'';path.oninput=()=>cap.path=path.value;const remove=document.createElement('button');remove.textContent='移除';remove.type='button';remove.onclick=()=>{step.capabilities.splice(i,1);render();};row.append(selected,type,path,remove);list.append(row);});
      const addCap=document.createElement('button');addCap.textContent='新增能力';addCap.type='button';addCap.onclick=()=>{step.capabilities.push({type:'reference',path:'',selected:false});render();};card.append(addCap);$('#steps').append(card);
    });
  }
  async function load() { const response=await fetch('/api/program');if(!response.ok)throw Error('無法讀取大綱'); const data=await response.json();revision=data.revision;outline=data.outline;$('#generated').textContent=data.generated||'尚未生成';$('#context').textContent=data.contextGenerated||'尚未生成';message(data.status);render(); }
  $('#add').onclick=()=>{outline.steps.push({id:`step-${outline.steps.length+1}`,title:'新步驟',selected:false,scripts:[],references:[],skills:[]});render();};
  $('#save').onclick=async()=>{const button=$('#save');button.disabled=true;try{if(!csrf){const r=await fetch('/api/csrf');csrf=await r.json();}const r=await fetch('/api/program',{method:'PUT',headers:{'Content-Type':'application/json',[csrf.headerName]:csrf.token},body:JSON.stringify({revision,outline})});const data=await r.json();if(!r.ok)throw Error(data.message||`保存失敗 (${r.status})`);revision=data.revision;$('#generated').textContent=data.generated;$('#context').textContent=data.contextGenerated;message(data.status);}catch(e){message(e.message,true);}finally{button.disabled=false;}};
  load().catch(e=>message(e.message,true));
})();
