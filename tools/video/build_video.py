#!/usr/bin/env python3
"""OnlyTrusted — real-UI walkthrough video generator.
=====================================================

Composites real iOS/Android screenshots into a branded multi-chapter
walkthrough MP4 with on-screen captions in 9 languages. Zero cloud —
runs entirely on your machine, no analytics, no outbound requests.

Dependencies:
    pip install Pillow
    brew install ffmpeg      # macOS
    apt install ffmpeg       # Linux

Usage:
    # English walkthrough (default)
    python build_video.py --lang en --video

    # Any shipped language
    python build_video.py --lang te --video     # Telugu
    python build_video.py --lang hi --video     # Hindi
    python build_video.py --lang es --video     # Spanish

    # All 9 locale caption tracks + 9 videos
    for lang in en es fr pt hi ar de id te; do
        python build_video.py --lang $lang --video
    done

Based on the OnlyTrusted Real UI Video Kit (external contributor, 2026-10-25).
Extended in-repo to add Telugu, chapter captures beyond dashboard, and a
longer-form walkthrough.

Add new screenshots to assets/ with these filenames (fallback to dashboard.png
if missing). Chapter order matches docs/USER_GUIDE_VIDEO_SCRIPT.md:
    splash.png            - Chapter 1 intro
    onboarding.png        - Chapter 3 setup
    consent.png           - Chapter 3 consent
    dashboard.png         - Chapter 4 dashboard tour (English)
    contacts.png          - Chapter 5 Trusted Contacts
    blocked.png           - Chapter 6 Blocked list
    log.png               - Chapter 7 Events log
    settings.png          - Chapter 8 Settings
    sos.png               - Chapter 9 SOS button
    smartlink.png         - Chapter 10 Smart Link Guard verdict
    language.png          - Chapter 14 Language & Region picker
    dashboard_telugu.png  - Chapter 14 Telugu localization demo
"""
import os, json, subprocess, argparse
from pathlib import Path
from PIL import Image, ImageDraw, ImageFont, ImageFilter

ROOT = Path(__file__).resolve().parent
W, H = 1280, 720

# Brand palette (Midnight) — mirrors ios/OnlyTrusted/Sources/Theme/
BG = '#0B111A'
PANEL = '#11192A'
LINE = '#1F3B47'
CYAN = '#55D9D4'
BRIGHT_CYAN = '#4FE3DF'
WHITE = '#F4FAFC'
MUTED = '#B6C9D2'
GOLD = '#F2B347'
DANGER = '#EF4444'

# Font paths — fallback chain. First existing wins per-locale.
FONT_CANDIDATES = {
    'default': [
        '/System/Library/Fonts/Helvetica.ttc',                      # macOS
        '/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf',          # Linux
        '/System/Library/Fonts/HelveticaNeue.ttc',                  # macOS fallback
    ],
    'hi': [
        # Sangam MN carries both Devanagari + Latin glyphs so mixed-script
        # subtitle text (e.g. "3 सेकंड, SMS + WhatsApp") renders without
        # tofu. Kohinoor + DevanagariMT are Devanagari-only — only use them
        # as last resort before Noto.
        '/System/Library/Fonts/Supplemental/Devanagari Sangam MN.ttc',
        '/System/Library/Fonts/Supplemental/DevanagariMT.ttc',
        '/System/Library/Fonts/Kohinoor.ttc',
        '/usr/share/fonts/truetype/noto/NotoSansDevanagari-Regular.ttf',
    ],
    'ar': [
        '/System/Library/Fonts/SFArabic.ttf',                       # macOS SF Arabic (shipped by default)
        '/System/Library/Fonts/SFArabicRounded.ttf',
        '/Library/Fonts/GeezaPro.ttc',                              # older macOS Arabic
        '/usr/share/fonts/truetype/noto/NotoSansArabic-Regular.ttf',
    ],
    'te': [
        # Telugu Sangam MN carries both Telugu + Latin glyphs so mixed
        # scripts render without tofu. KohinoorTelugu is Telugu-only.
        '/System/Library/Fonts/Supplemental/Telugu Sangam MN.ttc',
        '/System/Library/Fonts/KohinoorTelugu.ttc',
        '/System/Library/Fonts/SFIndia.ttc',
        '/usr/share/fonts/truetype/noto/NotoSansTelugu-Regular.ttf',
    ],
}

# Scene order: (key, duration_seconds, screenshot_fallback)
# key is used to find a matching PNG in assets/; if missing, falls back to the
# second-tuple screenshot. keeps the pipeline usable even when captures are
# incomplete — fewer dependencies on which iOS screens have been grabbed.
SCENES = [
    ('welcome', 4.0, 'splash'),
    ('setup', 4.0, 'onboarding'),
    ('consent', 3.0, 'consent'),
    ('dashboard', 5.0, 'dashboard'),
    ('protection', 4.0, 'dashboard'),
    ('contacts', 4.0, 'dashboard'),
    ('blocked', 4.0, 'dashboard'),
    ('log', 3.5, 'dashboard'),
    ('sos', 4.5, 'dashboard'),
    ('smartlink', 4.0, 'dashboard'),
    ('language', 4.5, 'dashboard_telugu'),
    ('honest', 4.0, 'dashboard'),
    ('end', 3.5, 'splash'),
]

# Localized captions. Each entry = (title, subtitle) per scene in SCENES order.
LANG = {
    'en': [
        ('OnlyTrusted',                 'Zero cloud. Zero account. Zero compromise.'),
        ('First-time setup',            'Email, PIN, Passkey. All on your device.'),
        ('Honest consent',              "You confirm what we can and can't do."),
        ('Your dashboard',              'Protection status, counts, and quick actions at a glance.'),
        ('Protection is active',        'Only your trusted contacts can reach you.'),
        ('Trusted Contacts',            'The people who matter get through. Everyone else is silenced.'),
        ('Blocked numbers',             'Block the scammers — one tap, logged on-device.'),
        ('Events log',                  'Every block, every alert — timestamped, local, auditable.'),
        ('Distress Signal',             '3-second countdown, location SMS + call + WhatsApp.'),
        ('Smart Link Guard',            '12 rules on every URL. Catches phishing before you tap.'),
        ('9 languages shipped',         'English, Spanish, French, Portuguese, Hindi, Arabic, German, Indonesian, Telugu.'),
        ('Honest Limits',               "We tell you what we can't do. Up front, in-app."),
        ('OnlyTrusted',                 'Install today. Yours forever on the Free tier.'),
    ],
    'es': [
        ('OnlyTrusted',                 'Cero nube. Cero cuenta. Cero compromiso.'),
        ('Configuración inicial',       'Correo, PIN, Passkey. Todo en tu dispositivo.'),
        ('Consentimiento honesto',      'Confirmas lo que podemos y no podemos hacer.'),
        ('Tu panel',                    'Estado, cifras y acciones rápidas de un vistazo.'),
        ('Protección activa',           'Solo tus contactos de confianza pueden contactarte.'),
        ('Contactos confiables',        'Las personas que importan pasan. El resto se silencia.'),
        ('Números bloqueados',          'Bloquea a los estafadores — un toque, registrado en el dispositivo.'),
        ('Registro de eventos',         'Cada bloqueo, cada alerta — con hora, local, auditable.'),
        ('Señal de auxilio',            'Cuenta regresiva 3 s, SMS con ubicación + llamada + WhatsApp.'),
        ('Smart Link Guard',            '12 reglas en cada URL. Detecta el phishing antes de tocar.'),
        ('9 idiomas',                   'Inglés, español, francés, portugués, hindi, árabe, alemán, indonesio, telugu.'),
        ('Límites honestos',            'Te decimos lo que no podemos hacer. De frente, en la app.'),
        ('OnlyTrusted',                 'Instálala hoy. Tuya para siempre en el plan Gratis.'),
    ],
    'fr': [
        ('OnlyTrusted',                 'Zéro cloud. Zéro compte. Zéro compromis.'),
        ('Première configuration',      'E-mail, PIN, Passkey. Tout sur votre appareil.'),
        ('Consentement honnête',        'Vous confirmez ce que nous pouvons et ne pouvons pas faire.'),
        ('Votre tableau de bord',       "Statut, chiffres et actions rapides d'un coup d'œil."),
        ('Protection active',           'Seuls vos contacts de confiance peuvent vous joindre.'),
        ('Contacts de confiance',       'Les personnes qui comptent passent. Les autres sont mises en silence.'),
        ('Numéros bloqués',             "Bloquez les escrocs — une touche, consigné sur l'appareil."),
        ('Journal des événements',      'Chaque blocage, chaque alerte — horodaté, local, auditable.'),
        ('Signal de détresse',          'Compte à rebours 3 s, SMS de localisation + appel + WhatsApp.'),
        ('Smart Link Guard',            '12 règles sur chaque URL. Détecte le phishing avant de toucher.'),
        ('9 langues',                   'Anglais, espagnol, français, portugais, hindi, arabe, allemand, indonésien, télougou.'),
        ('Limites honnêtes',            "On vous dit ce qu'on ne fait pas. Clairement, dans l'app."),
        ('OnlyTrusted',                 "Installez aujourd'hui. À vous pour toujours en Gratuit."),
    ],
    'pt': [
        ('OnlyTrusted',                 'Zero nuvem. Zero conta. Zero compromisso.'),
        ('Configuração inicial',        'E-mail, PIN, Passkey. Tudo no seu aparelho.'),
        ('Consentimento honesto',       'Você confirma o que podemos e não podemos fazer.'),
        ('Seu painel',                  'Status, contagens e ações rápidas em um relance.'),
        ('Proteção ativa',              'Só seus contatos de confiança podem te contatar.'),
        ('Contatos de confiança',       'As pessoas que importam passam. O resto é silenciado.'),
        ('Números bloqueados',          'Bloqueie golpistas — um toque, registrado no aparelho.'),
        ('Registro de eventos',         'Cada bloqueio, cada alerta — com hora, local, auditável.'),
        ('Sinal de socorro',            'Contagem de 3 s, SMS com localização + chamada + WhatsApp.'),
        ('Smart Link Guard',            '12 regras em cada URL. Pega phishing antes de você tocar.'),
        ('9 idiomas',                   'Inglês, espanhol, francês, português, hindi, árabe, alemão, indonésio, telugu.'),
        ('Limites honestos',            'Falamos o que não podemos fazer. De frente, no app.'),
        ('OnlyTrusted',                 'Instale hoje. Seu para sempre no plano Grátis.'),
    ],
    'hi': [
        ('OnlyTrusted',                 'कोई क्लाउड नहीं। कोई खाता नहीं। कोई समझौता नहीं।'),
        ('पहली बार सेटअप',               'ईमेल, PIN, Passkey। सब आपके डिवाइस पर।'),
        ('ईमानदार सहमति',                'आप पुष्टि करते हैं कि हम क्या कर सकते हैं और क्या नहीं।'),
        ('आपका डैशबोर्ड',                'सुरक्षा स्थिति, आँकड़े और त्वरित विकल्प एक ही जगह।'),
        ('सुरक्षा सक्रिय',               'केवल आपके विश्वसनीय संपर्क ही आप तक पहुँच सकते हैं।'),
        ('विश्वसनीय संपर्क',             'जो मायने रखते हैं वे पहुँचते हैं। बाकी शांत कर दिए जाते हैं।'),
        ('ब्लॉक किए गए नंबर',             'धोखेबाज़ों को ब्लॉक करें — एक टैप, डिवाइस पर दर्ज।'),
        ('ईवेंट लॉग',                     'हर ब्लॉक, हर अलर्ट — टाइमस्टैम्प, स्थानीय, जाँच योग्य।'),
        ('आपातकालीन संकेत',               '3 सेकंड काउंटडाउन, स्थान SMS + कॉल + WhatsApp।'),
        ('Smart Link Guard',             'हर URL पर 12 नियम। टैप करने से पहले फ़िशिंग पकड़ता है।'),
        ('9 भाषाएँ',                     'अंग्रेज़ी, स्पेनिश, फ़्रेंच, पुर्तगाली, हिन्दी, अरबी, जर्मन, इंडोनेशियाई, तेलुगु।'),
        ('ईमानदार सीमाएँ',                'हम बताते हैं कि क्या नहीं कर सकते। खुलकर, ऐप में।'),
        ('OnlyTrusted',                  'आज ही इंस्टॉल करें। Free स्तर पर हमेशा आपका।'),
    ],
    'ar': [
        ('OnlyTrusted',                 'لا سحابة. لا حساب. لا تنازلات.'),
        ('الإعداد الأول',               'بريد, PIN, Passkey. كل شيء على جهازك.'),
        ('موافقة صادقة',                'أنت تؤكد ما نستطيع فعله وما لا نستطيع.'),
        ('لوحة التحكم',                 'حالة الحماية، الأرقام والإجراءات السريعة بنظرة.'),
        ('الحماية نشطة',                'جهات اتصالك الموثوقة فقط تستطيع الوصول إليك.'),
        ('جهات اتصال موثوقة',           'الأشخاص المهمون يمرون. البقية يُكتَمون.'),
        ('أرقام محظورة',                'احظر المحتالين — بنقرة واحدة، مسجَّل على الجهاز.'),
        ('سجل الأحداث',                 'كل حظر، كل تنبيه — بالتوقيت، محلي، قابل للتدقيق.'),
        ('إشارة الاستغاثة',             'عد تنازلي 3 ثوانٍ, رسالة موقع + مكالمة + WhatsApp.'),
        ('Smart Link Guard',           '12 قاعدة على كل رابط. يلتقط التصيد قبل أن تنقر.'),
        ('9 لغات',                     'الإنجليزية والإسبانية والفرنسية والبرتغالية والهندية والعربية والألمانية والإندونيسية والتيلوغو.'),
        ('حدود صادقة',                  'نخبرك بما لا نستطيع فعله. صراحةً، داخل التطبيق.'),
        ('OnlyTrusted',                 'ثبّت اليوم. مجاني للأبد.'),
    ],
    'de': [
        ('OnlyTrusted',                 'Keine Cloud. Kein Konto. Keine Kompromisse.'),
        ('Erste Einrichtung',           'E-Mail, PIN, Passkey. Alles auf Ihrem Gerät.'),
        ('Ehrliche Einwilligung',       'Sie bestätigen, was wir können und was nicht.'),
        ('Ihr Dashboard',               'Schutzstatus, Zahlen und Schnellaktionen auf einen Blick.'),
        ('Schutz aktiv',                'Nur Ihre vertrauten Kontakte erreichen Sie.'),
        ('Vertraute Kontakte',          'Die Menschen, die zählen, kommen durch. Der Rest wird stummgeschaltet.'),
        ('Blockierte Nummern',          'Blockieren Sie Betrüger — ein Tipp, auf dem Gerät protokolliert.'),
        ('Ereignisprotokoll',           'Jede Blockierung, jede Warnung — mit Zeitstempel, lokal, prüfbar.'),
        ('Notfallsignal',               '3-Sek.-Countdown, Standort-SMS + Anruf + WhatsApp.'),
        ('Smart Link Guard',            '12 Regeln pro URL. Erkennt Phishing bevor Sie tippen.'),
        ('9 Sprachen',                  'Englisch, Spanisch, Französisch, Portugiesisch, Hindi, Arabisch, Deutsch, Indonesisch, Telugu.'),
        ('Ehrliche Grenzen',            'Wir sagen Ihnen, was wir nicht können. Direkt in der App.'),
        ('OnlyTrusted',                 'Heute installieren. In Free für immer Ihnen.'),
    ],
    'id': [
        ('OnlyTrusted',                 'Nol cloud. Nol akun. Nol kompromi.'),
        ('Pengaturan pertama',          'Email, PIN, Passkey. Semua di perangkat Anda.'),
        ('Persetujuan jujur',           'Anda mengonfirmasi apa yang bisa dan tidak bisa kami lakukan.'),
        ('Dasbor Anda',                 'Status perlindungan, angka, dan aksi cepat dalam sekilas.'),
        ('Perlindungan aktif',          'Hanya kontak tepercaya yang bisa menghubungi Anda.'),
        ('Kontak tepercaya',            'Orang yang penting tetap bisa menghubungi. Sisanya dibisukan.'),
        ('Nomor yang diblokir',         'Blokir penipu — satu ketukan, tercatat di perangkat.'),
        ('Log kejadian',                'Setiap pemblokiran, setiap peringatan — bercap waktu, lokal, dapat diaudit.'),
        ('Sinyal darurat',              'Hitung mundur 3 detik, SMS lokasi + panggilan + WhatsApp.'),
        ('Smart Link Guard',            '12 aturan di setiap URL. Menangkap phishing sebelum Anda mengetuk.'),
        ('9 bahasa',                    'Inggris, Spanyol, Prancis, Portugis, Hindi, Arab, Jerman, Indonesia, Telugu.'),
        ('Batas jujur',                 'Kami beri tahu apa yang tidak bisa kami lakukan. Langsung, di aplikasi.'),
        ('OnlyTrusted',                 'Pasang hari ini. Milik Anda selamanya di tingkat Gratis.'),
    ],
    'te': [
        ('OnlyTrusted',                 'క్లౌడ్ లేదు. ఖాతా లేదు. రాజీ లేదు.'),
        ('మొదటి సెటప్',                   'ఇమెయిల్, PIN, Passkey. అన్నీ మీ పరికరంపై.'),
        ('నిజాయితీ అంగీకారం',             'మేము ఏమి చేయగలము, ఏమి చేయలేము అని మీరు ధృవీకరిస్తారు.'),
        ('మీ డాష్బోర్డ్',                'సురక్ష స్థితి, సంఖ్యలు, త్వరిత చర్యలు ఒక్క చూపులో.'),
        ('సురక్ష సక్రియంగా ఉంది',          'మీ విశ్వసనీయ పరిచయస్థులు మాత్రమే మిమ్మల్ని చేరుకోగలరు.'),
        ('విశ్వసనీయ పరిచయస్థులు',          'ముఖ్యమైన వారు చేరతారు. మిగిలిన వారిని నిశ్శబ్దం చేస్తాం.'),
        ('బ్లాక్ చేసిన నంబర్లు',            'మోసగాళ్ళను బ్లాక్ చేయండి — ఒకే టాప్, పరికరంపై నమోదు.'),
        ('ఈవెంట్ లాగ్',                   'ప్రతి బ్లాక్, ప్రతి హెచ్చరిక — సమయంతో, స్థానికంగా, పరిశీలించదగినది.'),
        ('సహాయ సంకేతం',                 '3-సెకన్ల కౌంట్డౌన్, లొకేషన్ SMS + కాల్ + WhatsApp.'),
        ('Smart Link Guard',           'ప్రతి URL పై 12 నియమాలు. మీరు టాప్ చేయకముందే ఫిషింగ్ పట్టుకుంటుంది.'),
        ('9 భాషలు',                     'ఆంగ్లం, స్పానిష్, ఫ్రెంచ్, పోర్చుగీస్, హిందీ, అరబిక్, జర్మన్, ఇండోనేషియన్, తెలుగు.'),
        ('నిజాయితీ పరిమితులు',             'మేము ఏమి చేయలేము అని చెబుతాము. నేరుగా, యాప్ లోపల.'),
        ('OnlyTrusted',                  'ఈరోజు ఇన్స్టాల్ చేయండి. ఉచిత స్థాయిలో ఎప్పటికీ మీదే.'),
    ],
}

RTL_LANGS = {'ar', 'he', 'fa', 'ur'}


def pick_font(lang: str, size: int) -> ImageFont.FreeTypeFont:
    candidates = FONT_CANDIDATES.get(lang, []) + FONT_CANDIDATES['default']
    for path in candidates:
        if os.path.exists(path):
            try:
                return ImageFont.truetype(path, size)
            except Exception:
                continue
    return ImageFont.load_default()


def wrap_text(draw, text: str, font: ImageFont.FreeTypeFont, max_w: int):
    words = text.split()
    lines, cur = [], ''
    for word in words:
        trial = (cur + ' ' + word).strip()
        if cur and draw.textbbox((0, 0), trial, font=font)[2] > max_w:
            lines.append(cur)
            cur = word
        else:
            cur = trial
    if cur:
        lines.append(cur)
    return lines


_BACKDROP = None  # populated lazily by _load_backdrop()


def _load_backdrop() -> Image.Image:
    """Dimmed + blurred SplashHero used as the global video canvas so every
    scene sits inside the same mountain-lake atmosphere as the written user
    guide, instead of a flat navy card."""
    src = ROOT / 'assets' / 'splash.png'
    bg = Image.open(src).convert('RGB')
    # Cover-fit to canvas (crop any overhang after upscaling to cover).
    src_ratio = bg.width / bg.height
    tgt_ratio = W / H
    if src_ratio > tgt_ratio:
        new_h, new_w = H, int(H * src_ratio)
    else:
        new_w, new_h = W, int(W / src_ratio)
    bg = bg.resize((new_w, new_h), Image.Resampling.LANCZOS)
    left = (new_w - W) // 2
    top = (new_h - H) // 2
    bg = bg.crop((left, top, left + W, top + H))
    # Blur hides SplashHero copy ("OnlyTrusted", "Loading…") so it reads as
    # ambient backdrop, not a duplicate of the device screen next to it.
    bg = bg.filter(ImageFilter.GaussianBlur(radius=14))
    # Darken toward brand BG so WHITE title + MUTED subtitle keep contrast.
    overlay = Image.new('RGB', (W, H), BG)
    bg = Image.blend(bg, overlay, 0.62)
    return bg


def frame_for_scene(idx: int, lang: str) -> Image.Image:
    """Render a single frame for scene[idx] in [lang]."""
    global _BACKDROP
    key, _duration, fallback_shot = SCENES[idx]
    title, subtitle = LANG.get(lang, LANG['en'])[idx]

    if _BACKDROP is None:
        _BACKDROP = _load_backdrop()
    im = _BACKDROP.copy()
    d = ImageDraw.Draw(im)

    # Device frame — rounded rect that holds the screenshot
    d.rounded_rectangle((70, 42, 550, 678), radius=36, fill=PANEL, outline=CYAN, width=3)

    # Load screenshot. Try the chapter-specific one first, fall back to the
    # fallback_shot, then to dashboard, then to splash. Anything beats nothing.
    for candidate in [key, fallback_shot, 'dashboard', 'splash']:
        src = ROOT / 'assets' / f'{candidate}.png'
        if src.exists():
            break
    else:
        raise FileNotFoundError(f'no screenshot at {ROOT/"assets"}; capture at least dashboard.png')

    shot = Image.open(src).convert('RGB')
    # Fit shot inside the device frame with letterboxing
    frame_w, frame_h = 460, 596
    ratio = min(frame_w / shot.width, frame_h / shot.height)
    sw, sh = int(shot.width * ratio), int(shot.height * ratio)
    sx = 70 + (480 - sw) // 2
    sy = 56 + (608 - sh) // 2
    shot = shot.resize((sw, sh), Image.Resampling.LANCZOS)
    im.paste(shot, (sx, sy))

    # Right-column text
    d = ImageDraw.Draw(im)

    # Branded wordmark top-right — always Latin, so always use the default font
    # (locale-specific fonts like Kohinoor Telugu don't carry Latin glyphs, so
    # using pick_font(lang) here produces tofu for "Only" / "Trusted").
    d.text((628, 112), 'Only', font=pick_font('default', 28), fill=WHITE)
    d.text((704, 112), 'Trusted', font=pick_font('default', 28), fill=CYAN)
    d.rounded_rectangle((625, 190, 1190, 194), radius=2, fill=CYAN)

    # Title + subtitle. For RTL scripts we pass direction='rtl' so Pillow (via
    # libraqm/HarfBuzz) does bidi reordering + Arabic ligature shaping.
    direction = 'rtl' if lang in RTL_LANGS else 'ltr'

    title_lines = wrap_text(d, title, pick_font(lang, 44), 560)[:3]
    for i, line in enumerate(title_lines):
        y = 245 + i * 60
        if direction == 'rtl':
            # Right-anchor the text so RTL reads from the right edge of the panel.
            d.text((1190, y), line, font=pick_font(lang, 44), fill=WHITE,
                   anchor='ra', direction='rtl')
        else:
            d.text((625, y), line, font=pick_font(lang, 44), fill=WHITE)

    sub_y = 245 + len(title_lines) * 60 + 40
    sub_lines = wrap_text(d, subtitle, pick_font(lang, 23), 560)[:5]
    for i, line in enumerate(sub_lines):
        y = sub_y + i * 36
        if direction == 'rtl':
            d.text((1190, y), line, font=pick_font(lang, 23), fill=MUTED,
                   anchor='ra', direction='rtl')
        else:
            d.text((625, y), line, font=pick_font(lang, 23), fill=MUTED)

    # Chapter counter + honest-limits footer — both are Latin-only text
    d.text((625, 637), f'{idx + 1:02d} / {len(SCENES):02d}',
           font=pick_font('default', 20), fill=CYAN)
    footer = 'REAL app screens ,  on-device demo ,  no screens fabricated'
    d.text((70, 689), footer, font=pick_font('default', 14), fill='#8FA5B2')
    return im


def build_video(lang: str, out_path: Path, fps: int = 30):
    """Render each scene as PNG, pipe to ffmpeg to produce the MP4."""
    frames_dir = ROOT / f'_frames_{lang}'
    frames_dir.mkdir(exist_ok=True)
    # Clear any stale frames from a previous run
    for p in frames_dir.glob('*.png'):
        p.unlink()

    manifest = []
    for idx, (_key, duration, _) in enumerate(SCENES):
        frame_path = frames_dir / f'scene_{idx:02d}.png'
        img = frame_for_scene(idx, lang)
        img.save(frame_path, 'PNG', optimize=True)
        manifest.append((frame_path, duration))
        print(f'  scene {idx+1}/{len(SCENES)} ({_key}) rendered, {frame_path.name}')

    # Build an ffconcat demuxer file so each scene holds for its duration.
    concat_path = frames_dir / 'concat.txt'
    with open(concat_path, 'w') as f:
        for frame_path, duration in manifest:
            f.write(f"file '{frame_path.name}'\n")
            f.write(f'duration {duration}\n')
        # Repeat the final frame once so ffconcat holds the last image.
        f.write(f"file '{manifest[-1][0].name}'\n")

    out_path.parent.mkdir(parents=True, exist_ok=True)
    cmd = [
        'ffmpeg', '-y',
        '-f', 'concat', '-safe', '0', '-i', str(concat_path),
        '-vsync', 'vfr',
        '-vf', f'fps={fps},format=yuv420p',
        '-c:v', 'libx264', '-preset', 'medium', '-crf', '20',
        '-movflags', '+faststart',
        str(out_path),
    ]
    print(f'  ffmpeg, {out_path.name}')
    subprocess.run(cmd, check=True, cwd=str(frames_dir))
    print(f'DONE: {out_path} ({sum(d for _, d in manifest):.1f}s)')


def write_vtt(lang: str, out_path: Path):
    """WebVTT caption track that matches the scene durations."""
    out_path.parent.mkdir(parents=True, exist_ok=True)
    rows = ['WEBVTT', '']
    t = 0.0
    for idx, (_k, duration, _) in enumerate(SCENES):
        title, subtitle = LANG.get(lang, LANG['en'])[idx]
        start = _ts(t)
        end = _ts(t + duration)
        rows.append(f'{idx + 1}')
        rows.append(f'{start} --> {end}')
        rows.append(f'{title}')
        rows.append(f'{subtitle}')
        rows.append('')
        t += duration
    out_path.write_text('\n'.join(rows), encoding='utf-8')
    print(f'DONE: {out_path} ({t:.1f}s)')


def _ts(seconds: float) -> str:
    h = int(seconds // 3600)
    m = int((seconds % 3600) // 60)
    s = seconds % 60
    return f'{h:02d}:{m:02d}:{s:06.3f}'


def main():
    ap = argparse.ArgumentParser(description='OnlyTrusted — real-UI walkthrough video.')
    ap.add_argument('--lang', default='en', choices=list(LANG.keys()),
                    help='UI caption language (default: en)')
    ap.add_argument('--video', action='store_true',
                    help='Produce the MP4 (default: just the caption track)')
    ap.add_argument('--fps', type=int, default=30)
    args = ap.parse_args()

    out_dir = ROOT / 'output'
    vtt = out_dir / f'OnlyTrusted_{args.lang}.vtt'
    write_vtt(args.lang, vtt)

    if args.video:
        mp4 = out_dir / f'OnlyTrusted_Real_UI_{args.lang}.mp4'
        build_video(args.lang, mp4, args.fps)


if __name__ == '__main__':
    main()
