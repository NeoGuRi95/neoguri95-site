import { Editor } from '@tiptap/core';
import StarterKit from '@tiptap/starter-kit';
import Image from '@tiptap/extension-image';
import Link from '@tiptap/extension-link';
import { CharacterCount, Placeholder } from '@tiptap/extensions';

const DRAFT_KEY = 'neoguri95:write-draft';
const IMAGE_TYPES = ['image/png', 'image/jpeg', 'image/gif', 'image/webp'];
const MAX_IMAGE_BYTES = 10 * 1024 * 1024; // application.yml의 multipart 제한과 맞춘다

const commands = {
  paragraph: (c) => c.setParagraph(),
  h2: (c) => c.toggleHeading({ level: 2 }),
  h3: (c) => c.toggleHeading({ level: 3 }),
  bold: (c) => c.toggleBold(),
  italic: (c) => c.toggleItalic(),
  underline: (c) => c.toggleUnderline(),
  strike: (c) => c.toggleStrike(),
  code: (c) => c.toggleCode(),
  bulletList: (c) => c.toggleBulletList(),
  orderedList: (c) => c.toggleOrderedList(),
  blockquote: (c) => c.toggleBlockquote(),
  codeBlock: (c) => c.toggleCodeBlock(),
  hr: (c) => c.setHorizontalRule(),
  undo: (c) => c.undo(),
  redo: (c) => c.redo(),
};

const activeChecks = {
  paragraph: (e) => e.isActive('paragraph'),
  h2: (e) => e.isActive('heading', { level: 2 }),
  h3: (e) => e.isActive('heading', { level: 3 }),
  bold: (e) => e.isActive('bold'),
  italic: (e) => e.isActive('italic'),
  underline: (e) => e.isActive('underline'),
  strike: (e) => e.isActive('strike'),
  code: (e) => e.isActive('code'),
  link: (e) => e.isActive('link'),
  bulletList: (e) => e.isActive('bulletList'),
  orderedList: (e) => e.isActive('orderedList'),
  blockquote: (e) => e.isActive('blockquote'),
  codeBlock: (e) => e.isActive('codeBlock'),
};

const root = document.querySelector('[data-editor]');
if (root) {
  mount(root);
}

function mount(root) {
  const form = root.closest('form');
  const titleInput = form.querySelector('input[name="title"]');
  const htmlInput = form.querySelector('input[name="bodyHtml"]');
  const jsonInput = form.querySelector('input[name="bodyJson"]');
  const tokenInput = form.querySelector('input[name="token"]');
  const fileInput = root.querySelector('[data-image-input]');
  const buttons = [...root.querySelectorAll('[data-cmd]')];
  const linkbar = root.querySelector('[data-linkbar]');
  const linkInput = linkbar.querySelector('input');
  const count = root.querySelector('[data-editor-count]');
  const message = root.querySelector('[data-editor-message]');

  const restored = jsonInput.value ? null : loadDraft();
  if (restored?.title && !titleInput.value) {
    titleInput.value = restored.title;
  }

  const editor = new Editor({
    element: root.querySelector('[data-editor-content]'),
    extensions: [
      StarterKit.configure({ heading: { levels: [2, 3] }, link: false }),
      // autolink를 켜면 링크가 inclusive가 되어 링크 뒤에 친 글자까지 링크가 된다. 이를 막는다.
      Link.extend({ inclusive: () => false }).configure({
        openOnClick: false,
        autolink: true,
        defaultProtocol: 'https',
        HTMLAttributes: { rel: 'noopener noreferrer nofollow', target: null },
      }),
      Image,
      Placeholder.configure({ placeholder: '일상을 기록해 보세요.' }),
      CharacterCount,
    ],
    content: parseJson(jsonInput.value) ?? restored?.doc ?? '',
    editorProps: {
      attributes: {
        class: 'prose editor-prose',
        'aria-label': '본문',
        'aria-multiline': 'true',
        role: 'textbox',
      },
      handlePaste(view, event) {
        const files = imageFiles(event.clipboardData);
        if (files.length === 0) return false;
        upload(files);
        return true;
      },
      handleDrop(view, event, slice, moved) {
        if (moved) return false;
        const files = imageFiles(event.dataTransfer);
        if (files.length === 0) return false;
        event.preventDefault();
        const target = view.posAtCoords({ left: event.clientX, top: event.clientY });
        upload(files, target?.pos);
        return true;
      },
    },
    onTransaction: refresh,
    onUpdate: scheduleDraftSave,
  });

  if (restored) {
    say('저장해 둔 임시 글을 불러왔어요.');
  }
  refresh();

  // 툴바 버튼을 눌러도 본문 선택이 풀리지 않게 한다.
  root.querySelector('.editor-toolbar').addEventListener('mousedown', (event) => {
    if (event.target.closest('[data-cmd]')) event.preventDefault();
  });

  for (const button of buttons) {
    button.addEventListener('click', () => {
      const cmd = button.dataset.cmd;
      if (cmd === 'link') {
        toggleLinkbar();
      } else if (cmd === 'image') {
        fileInput.click();
      } else {
        commands[cmd](editor.chain().focus()).run();
      }
    });
  }

  fileInput.addEventListener('change', () => {
    upload([...fileInput.files]);
    fileInput.value = '';
  });

  linkbar.querySelector('[data-link-apply]').addEventListener('click', applyLink);
  linkbar.querySelector('[data-link-remove]').addEventListener('click', () => {
    editor.chain().focus().extendMarkRange('link').unsetLink().run();
    closeLinkbar();
  });
  linkInput.addEventListener('keydown', (event) => {
    if (event.key === 'Enter') {
      event.preventDefault();
      applyLink();
    } else if (event.key === 'Escape') {
      closeLinkbar();
      editor.commands.focus();
    }
  });

  titleInput.addEventListener('input', scheduleDraftSave);

  form.addEventListener('submit', (event) => {
    if (editor.isEmpty) {
      event.preventDefault();
      say('본문을 입력해 주세요.', true);
      editor.commands.focus();
      return;
    }
    htmlInput.value = editor.getHTML();
    jsonInput.value = JSON.stringify(editor.getJSON());
    clearDraft();
  });

  function refresh() {
    for (const button of buttons) {
      const check = activeChecks[button.dataset.cmd];
      if (check) button.setAttribute('aria-pressed', String(check(editor)));
    }
    root.querySelector('[data-cmd="undo"]').disabled = !editor.can().undo();
    root.querySelector('[data-cmd="redo"]').disabled = !editor.can().redo();
    count.textContent = `${editor.storage.characterCount.characters().toLocaleString('ko-KR')}자`;
  }

  function toggleLinkbar() {
    if (!linkbar.hidden) {
      closeLinkbar();
      return;
    }
    linkInput.value = editor.getAttributes('link').href ?? '';
    linkbar.hidden = false;
    linkInput.focus();
    linkInput.select();
  }

  function closeLinkbar() {
    linkbar.hidden = true;
  }

  function applyLink() {
    const href = normalizeUrl(linkInput.value);
    closeLinkbar();
    // 링크 입력창에서 본문으로 포커스를 바로 돌려, 이어 치는 글자가 본문에 들어가게 한다.
    editor.view.focus();
    const chain = editor.chain();
    if (!href) {
      chain.extendMarkRange('link').unsetLink().run();
    } else if (editor.state.selection.empty && !editor.isActive('link')) {
      chain.insertContent({ type: 'text', text: href, marks: [{ type: 'link', attrs: { href } }] }).run();
    } else {
      chain.extendMarkRange('link').setLink({ href }).run();
    }
  }

  async function upload(files, pos) {
    const images = files.filter((file) => IMAGE_TYPES.includes(file.type));
    if (images.length === 0) {
      say('PNG, JPG, GIF, WEBP 이미지만 올릴 수 있어요.', true);
      return;
    }
    if (images.some((file) => file.size > MAX_IMAGE_BYTES)) {
      say('이미지는 한 장에 10MB까지 올릴 수 있어요.', true);
      return;
    }
    if (!tokenInput.value) {
      say('이미지를 올리려면 아래 관리자 토큰을 먼저 입력해 주세요.', true);
      tokenInput.focus();
      return;
    }
    let at = pos;
    for (const file of images) {
      say(`${file.name} 올리는 중…`);
      try {
        const url = await uploadImage(file);
        const node = { type: 'image', attrs: { src: url, alt: '' } };
        const chain = editor.chain().focus();
        if (at == null) {
          chain.insertContent(node).run();
        } else {
          chain.insertContentAt(at, node).run();
          at = undefined;
        }
        say('');
      } catch (error) {
        say(error.message, true);
        return;
      }
    }
  }

  async function uploadImage(file) {
    const body = new FormData();
    body.append('file', file);
    let response;
    try {
      response = await fetch(root.dataset.uploadUrl, {
        method: 'POST',
        headers: { 'X-Admin-Token': tokenInput.value },
        body,
      });
    } catch {
      throw new Error('이미지를 올리지 못했어요. 네트워크를 확인해 주세요.');
    }
    const result = await response.json().catch(() => ({}));
    if (!response.ok || !result.url) {
      throw new Error(result.error ?? '이미지를 올리지 못했어요.');
    }
    return result.url;
  }

  function say(text, isError = false) {
    message.textContent = text;
    message.classList.toggle('is-error', isError);
  }

  let draftTimer;
  function scheduleDraftSave() {
    clearTimeout(draftTimer);
    draftTimer = setTimeout(() => {
      try {
        localStorage.setItem(DRAFT_KEY, JSON.stringify({ title: titleInput.value, doc: editor.getJSON() }));
      } catch {
        // 저장소를 쓸 수 없는 환경에서는 임시저장을 건너뛴다.
      }
    }, 800);
  }

  function clearDraft() {
    clearTimeout(draftTimer);
    try {
      localStorage.removeItem(DRAFT_KEY);
    } catch {
      // 무시
    }
  }
}

function loadDraft() {
  try {
    const draft = JSON.parse(localStorage.getItem(DRAFT_KEY));
    return draft?.doc ? draft : null;
  } catch {
    return null;
  }
}

function parseJson(value) {
  if (!value) return null;
  try {
    return JSON.parse(value);
  } catch {
    return null;
  }
}

function imageFiles(transfer) {
  return transfer ? [...transfer.files].filter((file) => file.type.startsWith('image/')) : [];
}

function normalizeUrl(value) {
  const trimmed = value.trim();
  if (!trimmed) return '';
  if (/^(https?:|mailto:)/i.test(trimmed)) return trimmed;
  if (/^[a-z][a-z0-9+.-]*:/i.test(trimmed)) return '';
  return `https://${trimmed}`;
}
