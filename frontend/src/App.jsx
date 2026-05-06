import { Fragment, useEffect, useMemo, useRef, useState } from "react";
import {
  AdminGroupsSection,
  AdminStudentsSection,
  AdminTeachersSection
} from "./pages/AdminSections";
import { AdminJournalSection } from "./pages/AdminJournalSection";
import { StudentProfile } from "./pages/StudentProfile";
import { TeacherProfile } from "./pages/TeacherProfile";

class Validators {
  static phoneByRegex(phone) {
    return /^\+375(25|29|33|44)\d{7}$/.test(normalizeBelarusPhone(phone));
  }

  static emailByRegex(email) {
    return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email);
  }
}

function normalizeBelarusPhone(value) {
  const digits = String(value || "").replace(/\D/g, "");
  const body = digits.startsWith("375") ? digits.slice(3, 12) : digits.slice(0, 9);
  return body ? `+375${body}` : "+375";
}

function formatBelarusPhone(value) {
  const normalized = normalizeBelarusPhone(value);
  const body = normalized.slice(4);
  const code = body.slice(0, 2);
  const first = body.slice(2, 5);
  const second = body.slice(5, 7);
  const third = body.slice(7, 9);
  return [
    "+375",
    code,
    first,
    second,
    third
  ].filter(Boolean).join("-");
}

function normalizeEmail(value) {
  return String(value || "").trim().toLowerCase();
}

function normalizeFullName(value) {
  return String(value || "").trim().replace(/\s+/g, " ").toLowerCase();
}

function sanitizeUserAccounts(rows) {
  if (!Array.isArray(rows)) return [];
  const dedupByEmail = new Map();
  rows.forEach((row, idx) => {
    const safe = row && typeof row === "object" ? row : {};
    const email = normalizeEmail(safe.email);
    if (!email) return;
    const normalized = {
      id: safe.id ?? null,
      fullName: String(safe.fullName || "").trim() || `Пользователь ${idx + 1}`,
      phone: formatBelarusPhone(safe.phone || "+375"),
      email,
      password: String(safe.password || ""),
      birthDate: String(safe.birthDate || "").trim()
    };
    // Keep the latest occurrence for the same email.
    dedupByEmail.set(email, normalized);
  });
  return [...dedupByEmail.values()];
}

class StorageService {
  getJson(key, fallback) {
    try {
      return JSON.parse(localStorage.getItem(key) || JSON.stringify(fallback));
    } catch {
      return fallback;
    }
  }

  setJson(key, value) {
    localStorage.setItem(key, JSON.stringify(value));
  }

  remove(key) {
    localStorage.removeItem(key);
  }
}

class UiUtils {
  static fullName(person) {
    return [person.lastName, person.firstName, person.middleName].filter(Boolean).join(" ");
  }

  static toDataUrl(file) {
    return new Promise((resolve) => {
      const reader = new FileReader();
      reader.onload = () => resolve(reader.result);
      reader.readAsDataURL(file);
    });
  }

  static days = ["Понедельник", "Вторник", "Среда", "Четверг", "Пятница", "Суббота"];
}

class ApiService {
  constructor(baseUrl = "") {
    this.baseUrl = baseUrl;
  }

  endpoint(path) {
    return `${this.baseUrl}${path}`;
  }

  async toRequestError(res, fallbackMessage) {
    let serverMessage = "";
    try {
      const data = await res.json();
      serverMessage = String(data?.message || "").trim();
    } catch {
      // ignore parse errors, fallback will be used
    }
    const error = new Error(serverMessage || fallbackMessage);
    error.status = res.status;
    throw error;
  }

  async fetchList(entity) {
    const res = await fetch(this.endpoint(`/${entity}`));
    if (!res.ok) {
      await this.toRequestError(res, `Request failed: GET /${entity} (${res.status})`);
    }
    return res.json();
  }

  async save(entity, id, payload) {
    const method = id ? "PUT" : "POST";
    const url = this.endpoint(id ? `/${entity}/${id}` : `/${entity}`);
    const res = await fetch(url, {
      method,
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(payload)
    });
    if (!res.ok) {
      await this.toRequestError(res, `Request failed: ${method} ${url} (${res.status})`);
    }
    if (res.status === 204) return null;
    return res.json();
  }

  async remove(entity, id) {
    const res = await fetch(this.endpoint(`/${entity}/${id}`), { method: "DELETE" });
    if (!res.ok) {
      await this.toRequestError(res, `Request failed: DELETE /${entity}/${id} (${res.status})`);
    }
  }
}

class AuthService {
  static ADMIN_LOGIN = "admin";
  static ADMIN_PASSWORD = "adgjl123098";

  constructor(validators) {
    this.validators = validators;
  }

  registerUser(accounts, students, teachers, form) {
    const normalizedName = form.fullName.trim();
    const normalizedPhone = normalizeBelarusPhone(form.phone);
    if (!normalizedName) {
      return { ok: false, message: "Введите ФИО." };
    }
    if (!this.validators.phoneByRegex(normalizedPhone)) {
      return { ok: false, message: "Телефон должен быть в формате +375-25-501-23-91." };
    }
    if (!this.validators.emailByRegex(form.email)) {
      return { ok: false, message: "Некорректный email." };
    }
    if (!String(form.password || "").trim()) {
      return { ok: false, message: "Введите пароль." };
    }
    if (accounts.some((a) => normalizeEmail(a.email) === normalizeEmail(form.email))) {
      return { ok: false, message: "Эта почта уже занята." };
    }
    if (accounts.some((a) => normalizeBelarusPhone(a.phone) === normalizedPhone)) {
      return { ok: false, message: "Этот номер уже занят." };
    }
    return {
      ok: true,
      account: {
        fullName: normalizedName,
        phone: formatBelarusPhone(normalizedPhone),
        email: form.email.trim(),
        password: form.password
      }
    };
  }

  loginUser(accounts, teachers, teacherAccessEmails, form) {
    const rawLogin = String(form.email || "").trim().toLowerCase();
    const account = accounts.find((a) => {
      const email = String(a.email || "").trim().toLowerCase();
      const loginPart = (email.split("@")[0] || "").toLowerCase();
      const byEmail = email === rawLogin;
      const byLogin = loginPart === rawLogin;
      return (byEmail || byLogin) && a.password === form.password;
    });
    if (!account) return { ok: false, message: "Неверный email или пароль." };
    const hasTeacherAccess = teacherAccessEmails.some((mail) => normalizeEmail(mail) === normalizeEmail(account.email));
    if (hasTeacherAccess) {
      return { ok: true, session: { role: "teacher", teacherName: account.fullName, email: account.email } };
    }
    return { ok: true, session: { role: "student", studentName: account.fullName, email: account.email } };
  }

  loginAdmin(form) {
    if (form.login !== AuthService.ADMIN_LOGIN || form.password !== AuthService.ADMIN_PASSWORD) {
      return { ok: false, message: "Неверный логин или пароль администратора." };
    }
    return { ok: true, session: { role: "admin", login: AuthService.ADMIN_LOGIN } };
  }
}

const storage = new StorageService();
const api = new ApiService(import.meta.env.VITE_API_URL || "");
const auth = new AuthService(Validators);
const ACADEMIC_HOURS_PER_ABSENCE = 2;
const hasExcuseReason = (reason) => {
  const normalized = String(reason || "").trim().toLowerCase();
  return Boolean(normalized) && normalized !== "отсутствовал(а)";
};
const GROUP_MEMBERS_PAGE_SIZE = 10;
const TEACHER_WORK_STATUSES = ["Активен", "В отпуске", "На больничном"];
const STUDENT_STATUSES = ["Активен", "Без группы", "Отчислен", "На больничном", "В академическом отпуске"];

function formatBirthRu(isoOrRaw) {
  if (!isoOrRaw) return "—";
  try {
    const d = new Date(`${isoOrRaw}T12:00:00`);
    return Number.isNaN(d.getTime()) ? isoOrRaw : d.toLocaleDateString("ru-RU");
  } catch {
    return isoOrRaw;
  }
}

function formatCourseDisplay(courseRaw) {
  if (courseRaw == null || String(courseRaw).trim() === "") return "Курс не указан";
  const s = String(courseRaw).trim();
  if (/курс/i.test(s)) return s;
  if (/^\d+(\.\d+)?$/.test(s)) return `${parseInt(s, 10)} курс`;
  return s;
}

function shortStudentName(fullName) {
  const p = (fullName || "").trim().split(/\s+/).filter(Boolean);
  if (p.length === 0) return "";
  const [last, ...rest] = p;
  const inits = rest.map((w) => (w[0] ? `${w[0].toUpperCase()}.` : "")).join("");
  return inits ? `${last} ${inits}` : last;
}

function buildLessonSlots() {
  const slots = [];
  let currentMinutes = 8 * 60 + 30;
  const endBoundary = 22 * 60;
  let number = 1;
  const fmt = (mins) => `${String(Math.floor(mins / 60)).padStart(2, "0")}:${String(mins % 60).padStart(2, "0")}`;
  while (currentMinutes + 85 <= endBoundary) {
    const start = currentMinutes;
    const end = start + 85;
    slots.push({ number, start: fmt(start), end: fmt(end) });
    number += 1;
    const breakMinutes = (slots.length % 2 === 0) ? 30 : 10;
    currentMinutes = end + breakMinutes;
  }
  return slots;
}

const LESSON_TYPES = [
  { code: "ЛК", key: "lecture", label: "ЛК", title: "Лекция", icon: "ЛК" },
  { code: "ПЗ", key: "practice", label: "ПЗ", title: "Практическое занятие", icon: "ПЗ" },
  { code: "ЛР", key: "lab", label: "ЛР", title: "Лабораторная работа", icon: "ЛР" }
];

const lessonTypeByCode = Object.fromEntries(LESSON_TYPES.map((type) => [type.code, type]));

const normalizeLessonType = (value, fallbackIndex = 0) => {
  if (lessonTypeByCode[value]) return value;
  return LESSON_TYPES[fallbackIndex % LESSON_TYPES.length].code;
};

const lessonTypeMeta = (value) => lessonTypeByCode[normalizeLessonType(value)];

const averageValue = (items, picker = (x) => x) => {
  const values = items.flatMap((item) => gradeNumbers(picker(item)));
  return values.length ? (values.reduce((sum, value) => sum + value, 0) / values.length).toFixed(1) : "—";
};

const gradeNumbers = (raw) => String(raw ?? "")
  .split(",")
  .map((part) => Number(part.trim().replace(",", ".")))
  .filter((value) => !Number.isNaN(value));

const isValidGradeInput = (raw) => {
  const values = gradeNumbers(raw);
  return values.length > 0 && values.every((value) => Number.isInteger(value) && value >= 0 && value <= 10);
};

const lessonKey = (lesson) => `${lesson.day}|${lesson.slot}|${lesson.group}`;

const dayShortName = (day) => ({
  Понедельник: "Пн",
  Вторник: "Вт",
  Среда: "Ср",
  Четверг: "Чт",
  Пятница: "Пт",
  Суббота: "Сб"
}[day] || day.slice(0, 2));

function SidebarIcon({ name }) {
  const iconPathByName = {
    home: "M4 11.5L12 5l8 6.5V20a1 1 0 0 1-1 1h-5v-5h-4v5H5a1 1 0 0 1-1-1v-8.5Z",
    calendar: "M7 3v3M17 3v3M4 9h16M6 6h12a2 2 0 0 1 2 2v10a2 2 0 0 1-2 2H6a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2Z",
    journal: "M6 4h10a3 3 0 0 1 3 3v13H9a3 3 0 0 0-3 3V4Zm0 0v19M9 8h7M9 12h7M9 16h5",
    students: "M16 20v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2M18 20v-2a4 4 0 0 0-2-3.46M15 4.7a4 4 0 1 1 0 7.8M12 7a4 4 0 1 1-8 0 4 4 0 0 1 8 0Z",
    teacher: "M12 4 3 8l9 4 7-3.1V14M6 9.4V14c0 2.8 2.7 5 6 5s6-2.2 6-5V9.4",
    discipline: "M5 4h12a2 2 0 0 1 2 2v14l-4-2-4 2-4-2-4 2V6a2 2 0 0 1 2-2Z",
    faculty: "M4 20h16M6 20V8l6-5 6 5v12M9 20v-7h6v7M8 10h.01M12 10h.01M16 10h.01",
    group: "M16 20v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2M18 20v-2a4 4 0 0 0-2-3.46M15 4.7a4 4 0 1 1 0 7.8M12 7a4 4 0 1 1-8 0 4 4 0 0 1 8 0Z",
    course: "M4 7l8-4 8 4-8 4-8-4Zm4 3v4c0 2 1.8 3.5 4 3.5s4-1.5 4-3.5v-4M20 9v6",
    grade: "M12 3l2.7 5.48L21 9.37l-4.5 4.38 1.06 6.2L12 17.1 6.44 19.95l1.06-6.2L3 9.37l6.3-.89L12 3Z",
    absence: "M4 12h16M6 6l12 12M18 6 6 18",
    edit: "M4 20h4l10.5-10.5a2.1 2.1 0 0 0-3-3L5 17v3Zm12-14 3 3",
    trash: "M4 7h16M9 7V5h6v2M7 7l1 13h8l1-13M10 11v5M14 11v5",
    check: "M5 13l4 4L19 7",
    open: "M7 17 17 7M9 7h8v8",
    access: "M12 2a5 5 0 0 0-5 5v3H6a2 2 0 0 0-2 2v8a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2v-8a2 2 0 0 0-2-2h-1V7a5 5 0 0 0-5-5Zm-3 8V7a3 3 0 0 1 6 0v3",
    support: "M4 5h16v11H7l-3 3V5Zm4 4h8M8 12h5",
    notes: "M6 4h12a2 2 0 0 1 2 2v14l-4-2-4 2-4-2-4 2V6a2 2 0 0 1 2-2Z",
    quote: "M9.4 8.2A3.2 3.2 0 0 0 6.2 11v1.8h3.3V20H3.5v-7.3A6.2 6.2 0 0 1 9.7 6.5h1.5v1.7H9.4Zm10.1 0A3.2 3.2 0 0 0 16.3 11v1.8h3.3V20h-6v-7.3a6.2 6.2 0 0 1 6.2-6.2h1.5v1.7h-1.8Z",
    settings: "M10.3 3.4h3.4l.7 2.1a6.7 6.7 0 0 1 1.5.9l2.1-.7 1.7 3-1.6 1.5a6.2 6.2 0 0 1 0 1.8l1.6 1.5-1.7 3-2.1-.7a6.7 6.7 0 0 1-1.5.9l-.7 2.1h-3.4l-.7-2.1a6.7 6.7 0 0 1-1.5-.9l-2.1.7-1.7-3 1.6-1.5a6.2 6.2 0 0 1 0-1.8L4.3 8.8l1.7-3 2.1.7a6.7 6.7 0 0 1 1.5-.9l.7-2.1ZM12 9.2a2.8 2.8 0 1 0 0 5.6 2.8 2.8 0 0 0 0-5.6Z",
    logout: "M15 4h4a2 2 0 0 1 2 2v12a2 2 0 0 1-2 2h-4M11 16l4-4-4-4M15 12H3"
  };
  const path = iconPathByName[name] || iconPathByName.home;
  return (
    <svg className="menu-svg-icon" viewBox="0 0 24 24" fill="none" aria-hidden="true">
      <path d={path} stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" />
    </svg>
  );
}

function ActionIcon({ name }) {
  return <SidebarIcon name={name} />;
}

function App() {
  const [students, setStudents] = useState([]);
  const [teachers, setTeachers] = useState([]);
  const [groups, setGroups] = useState([]);
  const [disciplines, setDisciplines] = useState([]);
  const [grades, setGrades] = useState([]);
  const [gradeLessonMeta, setGradeLessonMeta] = useState(() => storage.getJson("gradeLessonMeta", {}));
  const [avatars, setAvatars] = useState(() => storage.getJson("studentAvatars", {}));
  const [userAccounts, setUserAccounts] = useState(() => storage.getJson("userAccounts", []));
  const [teacherAccessEmails, setTeacherAccessEmails] = useState(() => storage.getJson("teacherAccessEmails", []));
  const [studentFeeds, setStudentFeeds] = useState(() => storage.getJson("studentFeeds", {}));
  const [customLessons, setCustomLessons] = useState(() => storage.getJson("customLessons", []));
  const [absences, setAbsences] = useState(() => storage.getJson("studentAbsences", []));
  const [studentProfiles, setStudentProfiles] = useState(() => storage.getJson("studentProfiles", {}));
  const [groupMeta, setGroupMeta] = useState(() => storage.getJson("groupMeta", {}));
  const [facultiesCatalog, setFacultiesCatalog] = useState(() => storage.getJson("facultiesCatalog", ["ФКСиС"]));
  const [facultyMeta, setFacultyMeta] = useState(() => storage.getJson("facultyMeta", { "ФКСиС": { fullName: "Факультет компьютерных систем и сетей" } }));
  const [specialtiesCatalog, setSpecialtiesCatalog] = useState(() => storage.getJson("specialtiesCatalog", { "ФКСиС": ["КИ (ВМСиС)"] }));
  const [specialtyMeta, setSpecialtyMeta] = useState(() => storage.getJson("specialtyMeta", { "ФКСиС__КИ (ВМСиС)": { fullName: "Компьютерная инженерия (вычислительные машины, системы и сети)" } }));
  const [specialtyDisciplines, setSpecialtyDisciplines] = useState(() => storage.getJson("specialtyDisciplines", []));
  const [disciplineMeta, setDisciplineMeta] = useState(() => storage.getJson("disciplineMeta", {}));
  const [studentQuotes, setStudentQuotes] = useState(() => storage.getJson("studentQuotes", {}));
  const [teacherProfiles, setTeacherProfiles] = useState(() => storage.getJson("teacherProfiles", {}));
  const [adminAuditLog, setAdminAuditLog] = useState(() => storage.getJson("adminAuditLog", []));

  const [message, setMessage] = useState("");
  const [authMode, setAuthMode] = useState("login");
  const [session, setSession] = useState(() => storage.getJson("activeSession", null));
  const [adminTab, setAdminTab] = useState(() => storage.getJson("adminTab", "dashboard"));
  const [studentTab, setStudentTab] = useState(() => storage.getJson("studentTab", "profile"));
  const [search, setSearch] = useState("");
  const [studentGradeFilters, setStudentGradeFilters] = useState({ semester: "spring", subject: "", lessonType: "" });
  const [feedDraft, setFeedDraft] = useState("");
  const [editingPostId, setEditingPostId] = useState(null);
  const [editingPostText, setEditingPostText] = useState("");
  const [openPostMenuId, setOpenPostMenuId] = useState(null);
  const [showNewNoteModal, setShowNewNoteModal] = useState(false);
  const [newNoteText, setNewNoteText] = useState("");
  const [scheduleSearch, setScheduleSearch] = useState("");
  const [studentHomeSearch] = useState("");
  const [studentTheme, setStudentTheme] = useState(() => storage.getJson("studentTheme", "pink"));
  const [teacherTab, setTeacherTab] = useState(() => storage.getJson("teacherTab", "profile"));
  const [teacherJournalContext, setTeacherJournalContext] = useState({ groupNumber: "", disciplineName: "", lessonType: "ЛК", date: new Date().toISOString().slice(0, 10) });
  const [teacherJournalMode, setTeacherJournalMode] = useState("grade");
  const [teacherJournalDrafts, setTeacherJournalDrafts] = useState({});
  const teacherJournalSavingRef = useRef({});
  const [teacherHomeEditOpen, setTeacherHomeEditOpen] = useState(false);
  const [teacherProfileDraft, setTeacherProfileDraft] = useState(null);
  const [teacherPreviewName, setTeacherPreviewName] = useState(null);
  const [teacherDisciplineModal, setTeacherDisciplineModal] = useState(null);
  const [selectedAdminFaculty, setSelectedAdminFaculty] = useState(() => storage.getJson("selectedAdminFaculty", "ФКСиС"));
  const [facultySearch, setFacultySearch] = useState("");
  const [facultyDraft, setFacultyDraft] = useState("");
  const [facultyFullDraft, setFacultyFullDraft] = useState("");
  const [editingFaculty, setEditingFaculty] = useState(null);
  const [specialtyDraft, setSpecialtyDraft] = useState("");
  const [specialtyFullDraft, setSpecialtyFullDraft] = useState("");
  const [editingSpecialty, setEditingSpecialty] = useState(null);
  const [pendingFacultyDelete, setPendingFacultyDelete] = useState(null);
  const [pendingSpecialtyDelete, setPendingSpecialtyDelete] = useState(null);
  const [studentPreviewName, setStudentPreviewName] = useState(null);
  const [adminFacultyFilter, setAdminFacultyFilter] = useState("");
  const [adminSpecialtyFilter, setAdminSpecialtyFilter] = useState("");
  const [adminGroupFilter, setAdminGroupFilter] = useState("");
  const [adminStudentsPage, setAdminStudentsPage] = useState(1);
  const [adminGroupedPage, setAdminGroupedPage] = useState(1);
  const [adminJournalPage, setAdminJournalPage] = useState(1);
  const [adminDisciplinesPage, setAdminDisciplinesPage] = useState(1);
  const [adminAccessPage, setAdminAccessPage] = useState(1);
  const [studentAbsencesPage, setStudentAbsencesPage] = useState(1);
  const [teacherJournalPage, setTeacherJournalPage] = useState(1);
  const [teacherLessonForm, setTeacherLessonForm] = useState({ editId: null, replacesKey: "", day: "Понедельник", slot: 1, discipline: "", group: "", room: "500-к.", lessonType: "ЛК" });
  const [adminLessonForm, setAdminLessonForm] = useState({ editId: null, replacesKey: "", day: "Понедельник", slot: 1, discipline: "", group: "", room: "500-к.", teacher: "", lessonType: "ЛК" });
  const [absenceForm, setAbsenceForm] = useState({ studentName: "", disciplineName: "", count: 1, reason: "", date: new Date().toISOString().slice(0, 10) });
  const [groupMembersPage, setGroupMembersPage] = useState(1);
  const [settingsForm, setSettingsForm] = useState({ phone: "+375", email: "", birthDate: "" });
  const [passwordForm, setPasswordForm] = useState({ oldPassword: "", newPassword: "", confirmPassword: "" });
  const [settingsMessage, setSettingsMessage] = useState("");
  const [adminJournalFilters, setAdminJournalFilters] = useState({ group: "", discipline: "", teacher: "", student: "", type: "all", period: "" });
  const [adminScheduleFilters, setAdminScheduleFilters] = useState({ group: "", teacher: "", discipline: "", room: "", viewType: "all", period: "week", query: "" });
  const [selectedAdminLessonId, setSelectedAdminLessonId] = useState(null);
  const studentAvatarFileRef = useRef(null);
  const localIdRef = useRef(1000000);
  const nextLocalId = () => {
    localIdRef.current += 1;
    return localIdRef.current;
  };

  const [userLogin, setUserLogin] = useState({ loginOrEmail: "", password: "" });
  const [resetPasswordForm, setResetPasswordForm] = useState({ email: "", newPassword: "", confirmPassword: "" });
  const [registerForm, setRegisterForm] = useState({
    lastName: "",
    firstName: "",
    middleName: "",
    birthDate: "",
    phone: "+375",
    email: "",
    password: ""
  });

  const [stForm, setStForm] = useState({
    editTarget: "",
    firstName: "",
    lastName: "",
    middleName: "",
    groupNumber: "",
    disciplineNames: [],
    course: "",
    faculty: "",
    specialty: "",
    birthDate: "",
    studentStatus: "Активен",
    sickLeaveFrom: "",
    sickLeaveTo: "",
    curator: "",
    starosta: ""
  });
  const [tForm, setTForm] = useState({ editTarget: "", firstName: "", lastName: "", middleName: "" });
  const [gForm, setGForm] = useState({ editTarget: "", number: "", course: "1 курс", faculty: "ФКСиС", specialty: "КИ (ВМСиС)", curator: "" });
  const [dForm, setDForm] = useState({ editTarget: "", name: "", fullName: "", teacherName: "", faculty: "ФКСиС", specialty: "КИ (ВМСиС)", course: "1 курс" });
  const loadAll = async () => {
    const [s, t, g, d, gr, accounts] = await Promise.all(
      ["students", "teachers", "groups", "disciplines", "grades", "accounts"].map((e) => api.fetchList(e))
    );
    setStudents(s);
    setTeachers(t);
    setGroups(g);
    setDisciplines(d);
    setGrades(gr);
    setUserAccounts(sanitizeUserAccounts(accounts));
  };

  useEffect(() => {
    loadAll().catch(() => setMessage("Не удалось загрузить API. Убедитесь, что backend запущен на :8080."));
  }, []);
  useEffect(() => {
    const intervalId = setInterval(() => {
      Promise.all([api.fetchList("students"), api.fetchList("groups"), api.fetchList("accounts")])
        .then(([nextStudents, nextGroups, nextAccounts]) => {
          setStudents(nextStudents);
          setGroups(nextGroups);
          setUserAccounts(sanitizeUserAccounts(nextAccounts));
        })
        .catch(() => {});
    }, 15000);
    return () => clearInterval(intervalId);
  }, []);
  useEffect(() => storage.setJson("userAccounts", userAccounts), [userAccounts]);
  useEffect(() => storage.setJson("studentTheme", studentTheme), [studentTheme]);
  useEffect(() => storage.setJson("gradeLessonMeta", gradeLessonMeta), [gradeLessonMeta]);
  useEffect(() => storage.setJson("studentAvatars", avatars), [avatars]);
  useEffect(() => storage.setJson("teacherAccessEmails", teacherAccessEmails), [teacherAccessEmails]);
  useEffect(() => storage.setJson("studentFeeds", studentFeeds), [studentFeeds]);
  useEffect(() => storage.setJson("customLessons", customLessons), [customLessons]);
  useEffect(() => storage.setJson("studentAbsences", absences), [absences]);
  useEffect(() => storage.setJson("studentProfiles", studentProfiles), [studentProfiles]);
  useEffect(() => storage.setJson("groupMeta", groupMeta), [groupMeta]);
  useEffect(() => storage.setJson("facultiesCatalog", facultiesCatalog), [facultiesCatalog]);
  useEffect(() => storage.setJson("facultyMeta", facultyMeta), [facultyMeta]);
  useEffect(() => storage.setJson("specialtiesCatalog", specialtiesCatalog), [specialtiesCatalog]);
  useEffect(() => storage.setJson("specialtyMeta", specialtyMeta), [specialtyMeta]);
  useEffect(() => storage.setJson("specialtyDisciplines", specialtyDisciplines), [specialtyDisciplines]);
  useEffect(() => storage.setJson("disciplineMeta", disciplineMeta), [disciplineMeta]);
  useEffect(() => storage.setJson("selectedAdminFaculty", selectedAdminFaculty), [selectedAdminFaculty]);
  useEffect(() => storage.setJson("studentQuotes", studentQuotes), [studentQuotes]);
  useEffect(() => storage.setJson("teacherProfiles", teacherProfiles), [teacherProfiles]);
  useEffect(() => storage.setJson("adminAuditLog", adminAuditLog), [adminAuditLog]);
  useEffect(() => {
    if (session) {
      storage.setJson("activeSession", session);
    } else {
      storage.remove("activeSession");
    }
  }, [session]);
  useEffect(() => storage.setJson("adminTab", adminTab), [adminTab]);
  useEffect(() => storage.setJson("studentTab", studentTab), [studentTab]);
  useEffect(() => storage.setJson("teacherTab", teacherTab), [teacherTab]);

  const avgMap = useMemo(() => {
    const map = {};
    grades.forEach((g) => {
      if (!map[g.studentName]) map[g.studentName] = [];
      map[g.studentName].push(...gradeNumbers(g.value));
    });
    Object.keys(map).forEach((k) => {
      const arr = map[k];
      map[k] = (arr.reduce((a, b) => a + b, 0) / arr.length).toFixed(2);
    });
    return map;
  }, [grades]);

  const lessonSlots = useMemo(() => buildLessonSlots(), []);

  const baseSchedule = useMemo(() => {
    return [];
  }, []);

  const fullSchedule = useMemo(
    () => [
      ...baseSchedule.filter((lesson) => !customLessons.some((custom) => custom.replacesKey === lessonKey(lesson))),
      ...customLessons.filter((lesson) => !lesson.cancelled)
    ].map((lesson, index) => ({
      ...lesson,
      lessonType: normalizeLessonType(lesson.lessonType, Number(lesson.slot || index) - 1),
      typeMeta: lessonTypeMeta(lesson.lessonType || normalizeLessonType(null, Number(lesson.slot || index) - 1))
    })),
    [baseSchedule, customLessons]
  );

  const teacherIdByName = (name) => teachers.find((t) => UiUtils.fullName(t) === name)?.id;
  const studentIdByName = (name) => students.find((s) => UiUtils.fullName(s) === name)?.id;
  const groupIdByNumber = (num) => groups.find((g) => g.number === num)?.id;
  const disciplineIdByName = (name) => disciplines.find((d) => d.name === name)?.id;
  const gradeMetaKey = (studentName, disciplineName) => `${studentName}__${disciplineName}`;
  const gradeRecordMetaKey = (grade) => (grade?.id ? `grade:${grade.id}` : gradeMetaKey(grade?.studentName, grade?.disciplineName));
  const gradeMetaOf = (grade) => {
    if (grade?.id) return gradeLessonMeta[gradeRecordMetaKey(grade)] || {};
    return gradeLessonMeta[gradeMetaKey(grade?.studentName, grade?.disciplineName)] || {};
  };
  const removeGradeMetaKeys = (items) => {
    setGradeLessonMeta((prev) => {
      const next = { ...prev };
      items.forEach((grade) => {
        delete next[gradeRecordMetaKey(grade)];
        if (!grade?.id) delete next[gradeMetaKey(grade?.studentName, grade?.disciplineName)];
      });
      return next;
    });
  };
  const gradesForJournalCell = ({ studentName, disciplineName, lessonType, date }) => grades.filter((grade) => {
    const meta = gradeMetaOf(grade);
    return grade.studentName === studentName
      && grade.disciplineName === disciplineName
      && normalizeLessonType(meta.lessonType) === normalizeLessonType(lessonType)
      && meta.date === date;
  });
  const deleteJournalGrades = async (items) => {
    for (const grade of items) {
      if (grade.id) await api.remove("grades", grade.id);
    }
    removeGradeMetaKeys(items);
  };
  const hasAbsenceOnDate = ({ studentName, disciplineName, lessonType, date }) => absences.some((absence) => (
    absence.studentName === studentName
    && absence.disciplineName === disciplineName
    && normalizeLessonType(absence.lessonType) === normalizeLessonType(lessonType)
    && absence.date === date
    && (Number(absence.count || 0) > 0 || Number(absence.excusedHours || 0) > 0)
  ));
  const filtered = (items, mapper) => items.filter((it) => mapper(it).toLowerCase().includes(search.toLowerCase()));
  const logout = () => {
    storage.remove("activeSession");
    setSession(null);
  };

  const doUserRegister = async () => {
    if (!registerForm.lastName.trim() || !registerForm.firstName.trim() || !registerForm.middleName.trim()) {
      setMessage("Заполните фамилию, имя и отчество.");
      return;
    }
    if (!registerForm.birthDate) {
      setMessage("Укажите дату рождения.");
      return;
    }
    const passwordError = validatePassword(registerForm.password, registerForm.email);
    if (passwordError) {
      setMessage(passwordError);
      return;
    }
    const fullName = `${registerForm.lastName} ${registerForm.firstName} ${registerForm.middleName}`.trim();
    const result = auth.registerUser(userAccounts, students, teachers, { ...registerForm, fullName });
    if (!result.ok) return setMessage(result.message);
    const existingStudent = students.find((student) => normalizeFullName(UiUtils.fullName(student)) === normalizeFullName(fullName));
    const createdAccount = { ...result.account, birthDate: registerForm.birthDate || "" };
    const existingAcademic = existingStudent?.groupNumber ? groupAcademicOfNumber(existingStudent.groupNumber) : {};
    const fallbackDisciplineIds = disciplines.slice(0, 2).map((discipline) => discipline.id).filter(Boolean);
    if (!existingStudent) {
      try {
        await api.save("students", null, {
          firstName: registerForm.firstName.trim(),
          lastName: registerForm.lastName.trim(),
          middleName: registerForm.middleName.trim(),
          groupId: null,
          disciplineIds: fallbackDisciplineIds
        });
        await loadAll();
      } catch (error) {
        if (error?.status !== 409) {
          setMessage(error?.message || "Не удалось добавить студента в список администратора.");
          return;
        }
      }
    }
    try {
      const latestStudents = await api.fetchList("students").catch(() => students);
      const linkedStudent = latestStudents.find((student) => normalizeFullName(UiUtils.fullName(student)) === normalizeFullName(fullName));
      const canonicalFullName = linkedStudent ? UiUtils.fullName(linkedStudent) : fullName;
      createdAccount.fullName = canonicalFullName;
      await api.save("accounts", null, createdAccount);
      setSession({ role: "student", studentName: canonicalFullName, email: createdAccount.email });
    } catch (error) {
      setMessage(error?.message || "Не удалось сохранить аккаунт. Повторите попытку.");
      return;
    }
    setStudentProfiles((prev) => ({
      ...prev,
      [createdAccount.fullName]: {
        ...(prev[createdAccount.fullName] || {}),
        course: existingAcademic.course || prev[createdAccount.fullName]?.course || "",
        faculty: existingAcademic.faculty || prev[createdAccount.fullName]?.faculty || "",
        specialty: existingAcademic.specialty || prev[createdAccount.fullName]?.specialty || "",
        birthDate: registerForm.birthDate || prev[createdAccount.fullName]?.birthDate || ""
      }
    }));
    await loadAll();
    setRegisterForm({
      lastName: "",
      firstName: "",
      middleName: "",
      birthDate: "",
      phone: "+375",
      email: "",
      password: ""
    });
    setMessage("");
  };

  const ensureStudentRecordForAccount = async (account) => {
    const fullName = account?.fullName || "";
    if (!fullName) return;
    const split = parseFullName(fullName);
    const latestStudents = await api.fetchList("students").catch(() => students);
    const existingStudent = latestStudents.find((student) => normalizeFullName(UiUtils.fullName(student)) === normalizeFullName(fullName));
    const academic = existingStudent?.groupNumber ? groupAcademicOfNumber(existingStudent.groupNumber) : {};
    if (existingStudent?.groupNumber) {
      setStudentProfiles((prev) => ({
        ...prev,
        [fullName]: {
          ...(prev[fullName] || {}),
          course: academic.course || prev[fullName]?.course || "",
          faculty: academic.faculty || prev[fullName]?.faculty || "",
          specialty: academic.specialty || prev[fullName]?.specialty || "",
          birthDate: account.birthDate || prev[fullName]?.birthDate || ""
        }
      }));
      return;
    }
    if (existingStudent) return;
    const fallbackDisciplineIds = disciplines.slice(0, 2).map((discipline) => discipline.id).filter(Boolean);
    await api.save("students", null, {
      firstName: split.firstName,
      lastName: split.lastName,
      middleName: split.middleName,
      groupId: null,
      disciplineIds: fallbackDisciplineIds
    });
    await loadAll();
  };

  const doUserLogin = async () => {
    const accountResult = auth.loginUser(userAccounts, teachers, teacherAccessEmails, { email: userLogin.loginOrEmail, password: userLogin.password });
    if (accountResult.ok) {
      if (accountResult.session.role === "student") {
        const login = String(userLogin.loginOrEmail || "").trim().toLowerCase();
        const account = userAccounts.find((a) => {
          const email = String(a.email || "").trim().toLowerCase();
          const loginPart = (email.split("@")[0] || "").toLowerCase();
          return email === login || loginPart === login;
        });
        try {
          await ensureStudentRecordForAccount(account);
        } catch {
          setMessage("Не удалось обновить запись студента. Попробуйте ещё раз или проверьте backend.");
          return;
        }
      }
      setSession(accountResult.session);
      setUserLogin({ loginOrEmail: "", password: "" });
      setMessage("");
      return;
    }
    const adminResult = auth.loginAdmin({ login: userLogin.loginOrEmail, password: userLogin.password });
    if (!adminResult.ok) return setMessage("Неверный логин/email или пароль.");
    setSession(adminResult.session);
    setUserLogin({ loginOrEmail: "", password: "" });
    setMessage("");
  };

  const validatePassword = (next, email) => {
    const hasUpper = /[A-Z]/.test(next);
    const hasLower = /[a-z]/.test(next);
    const hasDigit = /\d/.test(next);
    const loginPart = (String(email || "").split("@")[0] || "").toLowerCase();
    if (next.length < 8 || next.length > 30) return "Пароль должен быть не меньше 8 символов.";
    if (!/^[A-Za-z0-9]+$/.test(next)) return "Пароль должен быть на латинице и содержать только буквы и цифры.";
    if (!(hasUpper && hasLower && hasDigit)) return "Пароль должен содержать прописные и обычные латинские буквы, а также цифры.";
    if (loginPart && next.toLowerCase().includes(loginPart)) return "Пароль не должен содержать логин.";
    return "";
  };

  const saveAccountToApi = async (account) => {
    if (!account) return null;
    if (account.id) {
      return api.save("accounts", account.id, account);
    }
    return api.save("accounts", null, account);
  };

  const resetPasswordByEmail = async () => {
    const email = resetPasswordForm.email.trim().toLowerCase();
    const account = userAccounts.find((a) => normalizeEmail(a.email) === email);
    if (!Validators.emailByRegex(resetPasswordForm.email)) return setMessage("Введите корректный email.");
    if (!account) return setMessage("Почта не зарегистрирована.");
    const passwordError = validatePassword(resetPasswordForm.newPassword, account.email);
    if (passwordError) return setMessage(passwordError);
    if (resetPasswordForm.newPassword === account.password) return setMessage("Новый пароль не должен совпадать со старым.");
    if (resetPasswordForm.newPassword !== resetPasswordForm.confirmPassword) return setMessage("Подтверждение пароля не совпадает.");
    const updatedAccount = { ...account, password: resetPasswordForm.newPassword };
    try {
      await saveAccountToApi(updatedAccount);
      await loadAll();
    } catch {
      setMessage("Не удалось обновить пароль на сервере.");
      return;
    }
    setResetPasswordForm({ email: "", newPassword: "", confirmPassword: "" });
    setAuthMode("login");
    setMessage("Пароль обновлен. Теперь войдите с новым паролем.");
  };

  const updateProfileSettings = async () => {
    if (!session?.email) return;
    if (!Validators.emailByRegex(settingsForm.email)) return setSettingsMessage("Введите корректный email.");
    const normalizedPhone = normalizeBelarusPhone(settingsForm.phone);
    if (!Validators.phoneByRegex(settingsForm.phone)) return setSettingsMessage("Телефон должен быть в формате +375-25-501-23-91.");
    const isEmailBusy = userAccounts.some((a) => normalizeEmail(a.email) === normalizeEmail(settingsForm.email) && normalizeEmail(a.email) !== normalizeEmail(session.email));
    if (isEmailBusy) return setSettingsMessage("Эта почта уже занята.");
    const isPhoneBusy = userAccounts.some((a) => normalizeBelarusPhone(a.phone) === normalizedPhone && normalizeEmail(a.email) !== normalizeEmail(session.email));
    if (isPhoneBusy) return setSettingsMessage("Этот номер уже занят.");
    const account = userAccounts.find((a) => normalizeEmail(a.email) === normalizeEmail(session.email));
    if (!account) return setSettingsMessage("Аккаунт не найден.");
    try {
      await saveAccountToApi({
        ...account,
        email: settingsForm.email.trim(),
        phone: formatBelarusPhone(normalizedPhone),
        birthDate: settingsForm.birthDate || account.birthDate || ""
      });
      await loadAll();
    } catch {
      setSettingsMessage("Не удалось обновить профиль на сервере.");
      return;
    }
    setSession((prev) => ({ ...prev, email: settingsForm.email.trim() }));
    if (session?.studentName && settingsForm.birthDate) {
      setStudentProfiles((prev) => ({
        ...prev,
        [session.studentName]: {
          ...(prev[session.studentName] || {}),
          birthDate: prev[session.studentName]?.birthDate || settingsForm.birthDate
        }
      }));
    }
    setSettingsMessage("Контакты обновлены.");
  };

  const updatePassword = async () => {
    if (!session?.email) return;
    const account = userAccounts.find((a) => normalizeEmail(a.email) === normalizeEmail(session.email));
    if (!account) return;
    const next = passwordForm.newPassword;
    if (!passwordForm.oldPassword) return setSettingsMessage("Введите текущий пароль.");
    if (passwordForm.oldPassword !== account.password) return setSettingsMessage("Старый пароль неверный.");
    const passwordError = validatePassword(next, session.email);
    if (passwordError) return setSettingsMessage(passwordError);
    if (next === account.password) return setSettingsMessage("Новый пароль не должен совпадать со старым.");
    if (next !== passwordForm.confirmPassword) return setSettingsMessage("Подтверждение пароля не совпадает.");
    try {
      await saveAccountToApi({ ...account, password: next });
      await loadAll();
    } catch {
      setSettingsMessage("Не удалось обновить пароль на сервере.");
      return;
    }
    setPasswordForm({ oldPassword: "", newPassword: "", confirmPassword: "" });
    setSettingsMessage("Пароль обновлен.");
  };

  const fillStudentFormByName = (name) => {
    const s = students.find((it) => UiUtils.fullName(it) === name);
    const meta = s ? (studentProfiles[UiUtils.fullName(s)] || {}) : {};
    const groupInfo = s?.groupNumber ? (groupMeta[String(s.groupNumber)] || {}) : {};
    setStForm(s ? {
      editTarget: name,
      firstName: s.firstName || "",
      lastName: s.lastName || "",
      middleName: s.middleName || "",
      groupNumber: s.groupNumber || "",
      disciplineNames: s.disciplines || [],
      course: meta.course || "",
      faculty: meta.faculty || "",
      specialty: meta.specialty || "",
      birthDate: meta.birthDate || "",
      studentStatus: meta.studentStatus || "Активен",
      sickLeaveFrom: meta.sickLeaveFrom || "",
      sickLeaveTo: meta.sickLeaveTo || "",
      curator: groupInfo.curator || "",
      starosta: groupInfo.starosta || ""
    } : {
      editTarget: "",
      firstName: "",
      lastName: "",
      middleName: "",
      groupNumber: "",
      disciplineNames: [],
      course: "",
      faculty: "",
      specialty: "",
      birthDate: "",
      studentStatus: "Активен",
      sickLeaveFrom: "",
      sickLeaveTo: "",
      curator: "",
      starosta: ""
    });
  };
  const fillTeacherFormByName = (name) => {
    const t = teachers.find((it) => UiUtils.fullName(it) === name);
    setTForm(t ? { editTarget: name, firstName: t.firstName || "", lastName: t.lastName || "", middleName: t.middleName || "" } : { editTarget: "", firstName: "", lastName: "", middleName: "" });
  };
  const fillGroupFormByNumber = (number) => {
    const meta = groupAcademicOfNumber(number);
    setGForm({
      editTarget: number || "",
      number: number || "",
      course: meta.course || "1 курс",
      faculty: meta.faculty || facultiesCatalog[0] || "",
      specialty: meta.specialty || specialtiesCatalog[meta.faculty || facultiesCatalog[0]]?.[0] || "",
      curator: meta.curator || ""
    });
  };
  const fillDisciplineFormByName = (name) => {
    const d = disciplines.find((it) => it.name === name);
    const meta = disciplineMeta[name] || {};
    const local = specialtyDisciplines.find((it) => it.name === name) || {};
    setDForm(d || local ? {
      editTarget: name,
      name: d?.name || local.name || "",
      fullName: meta.fullName || local.fullName || disciplineFullName(d?.name || local.name || ""),
      teacherName: d?.teacherName || "",
      faculty: meta.faculty || local.faculty || facultiesCatalog[0] || "",
      specialty: meta.specialty || local.specialty || specialtiesCatalog[meta.faculty || local.faculty || facultiesCatalog[0]]?.[0] || "",
      course: meta.course || local.course || "1 курс"
    } : { editTarget: "", name: "", fullName: "", teacherName: "", faculty: facultiesCatalog[0] || "", specialty: specialtiesCatalog[facultiesCatalog[0]]?.[0] || "", course: "1 курс" });
  };

  const saveEntity = async (entity, id, payload, resetFn) => {
    const saved = await api.save(entity, id, payload);
    await loadAll();
    resetFn();
    return saved;
  };

  const saveGroup = async () => {
    const number = String(gForm.number || "").trim();
    if (!number || !gForm.faculty || !gForm.specialty || !gForm.course) return setMessage("Заполните название группы, курс, факультет и специальность.");
    const id = groups.find((g) => g.number === gForm.editTarget)?.id;
    await api.save("groups", id, {
      number,
      course: gForm.course,
      faculty: gForm.faculty,
      specialty: gForm.specialty
    });
    setGroupMeta((prev) => {
      const next = { ...prev };
      if (gForm.editTarget && gForm.editTarget !== number) {
        next[number] = { ...(next[gForm.editTarget] || {}) };
        delete next[gForm.editTarget];
      }
      next[number] = {
        ...(next[number] || {}),
        course: gForm.course,
        faculty: gForm.faculty,
        specialty: gForm.specialty,
        curator: gForm.curator || ""
      };
      return next;
    });
    const oldGroup = groups.find((g) => g.number === gForm.editTarget);
    const affectedNames = (oldGroup?.students || []).map(String);
    if (affectedNames.length > 0) {
      setStudentProfiles((prev) => {
        const next = { ...prev };
        affectedNames.forEach((name) => {
          next[name] = {
            ...(next[name] || {}),
            course: gForm.course,
            faculty: gForm.faculty,
            specialty: gForm.specialty,
            studentStatus: next[name]?.studentStatus === "Без группы" ? "Активен" : (next[name]?.studentStatus || "Активен")
          };
        });
        return next;
      });
    }
    await loadAll();
    setGForm({ editTarget: "", number: "", course: "1 курс", faculty: gForm.faculty, specialty: gForm.specialty, curator: "" });
    setMessage(id ? "Группа обновлена." : "Группа создана.");
  };
  const deleteGroup = async (number = gForm.editTarget) => {
    const target = groups.find((g) => g.number === number);
    const id = target?.id;
    if (!id) return;
    const affectedNames = (target.students || []).map(String);
    await api.remove("groups", id);
    setGroupMeta((prev) => {
      const next = { ...prev };
      delete next[String(number)];
      return next;
    });
    setStudentProfiles((prev) => {
      const next = { ...prev };
      affectedNames.forEach((name) => {
        next[name] = {
          ...(next[name] || {}),
          studentStatus: "Без группы"
        };
      });
      return next;
    });
    await loadAll();
    setGForm({ editTarget: "", number: "", course: "1 курс", faculty: facultiesCatalog[0] || "", specialty: specialtiesCatalog[facultiesCatalog[0]]?.[0] || "", curator: "" });
    setMessage(`Группа ${number} удалена, студенты отвязаны от группы.`);
  };

  const saveTeacher = async () => {
    const id = teachers.find((t) => UiUtils.fullName(t) === tForm.editTarget)?.id;
    const nextName = `${tForm.lastName} ${tForm.firstName} ${tForm.middleName}`.trim();
    await saveEntity("teachers", id, { firstName: tForm.firstName, lastName: tForm.lastName, middleName: tForm.middleName }, () => setTForm({ editTarget: "", firstName: "", lastName: "", middleName: "" }));
    if (tForm.editTarget && nextName && tForm.editTarget !== nextName) {
      setTeacherProfiles((prev) => {
        const next = { ...prev, [nextName]: { ...(prev[tForm.editTarget] || {}) } };
        delete next[tForm.editTarget];
        return next;
      });
      setCustomLessons((prev) => prev.map((lesson) => lesson.teacher === tForm.editTarget ? { ...lesson, teacher: nextName } : lesson));
    }
  };
  const deleteTeacher = async () => {
    const id = teachers.find((t) => UiUtils.fullName(t) === tForm.editTarget)?.id;
    if (!id) return;
    await api.remove("teachers", id);
    await loadAll();
  };

  const deleteTeacherByName = async (fullName) => {
    const target = teachers.find((t) => UiUtils.fullName(t) === fullName);
    if (!target?.id) return;
    await api.remove("teachers", target.id);
    await loadAll();
    setMessage(`Преподаватель удалён: ${fullName}`);
  };

  const bulkSetTeacherStatus = async (teacherNames, status) => {
    setTeacherProfiles((prev) => {
      const next = { ...prev };
      teacherNames.forEach((name) => {
        next[name] = {
          ...(next[name] || {}),
          workStatus: status
        };
      });
      return next;
    });
    setMessage(`Статус "${status}" назначен (${teacherNames.length}).`);
  };

  const bulkDeleteTeachers = async (teacherNames) => {
    for (const fullName of teacherNames) {
      const target = teachers.find((t) => UiUtils.fullName(t) === fullName);
      if (target?.id) await api.remove("teachers", target.id);
    }
    await loadAll();
    setMessage(`Удалено преподавателей: ${teacherNames.length}.`);
  };

  const assignTeacherCurator = (teacherName, groupNumber) => {
    if (!teacherName || !groupNumber) return setMessage("Выберите преподавателя и группу.");
    setGroupMeta((prev) => ({
      ...prev,
      [String(groupNumber)]: {
        ...(prev[String(groupNumber)] || {}),
        curator: teacherName
      }
    }));
    setMessage(`${teacherName} назначен куратором группы ${groupNumber}.`);
  };

  const importTeachersBatch = async (items) => {
    let savedCount = 0;
    for (const row of items) {
      const firstName = String(row.firstName || "").trim();
      const lastName = String(row.lastName || "").trim();
      const middleName = String(row.middleName || "").trim();
      if (!firstName || !lastName) continue;
      const fullName = `${lastName} ${firstName} ${middleName}`.trim();
      const id = teachers.find((teacher) => UiUtils.fullName(teacher) === fullName)?.id;
      await api.save("teachers", id, { firstName, lastName, middleName });
      savedCount += 1;
      if (row.workStatus) {
        setTeacherProfiles((prev) => ({
          ...prev,
          [fullName]: {
            ...(prev[fullName] || {}),
            workStatus: row.workStatus
          }
        }));
      }
      if (row.curatorGroup) assignTeacherCurator(fullName, row.curatorGroup);
    }
    await loadAll();
    setMessage(`Сохранено преподавателей: ${savedCount}.`);
  };

  const saveDiscipline = async () => {
    const name = dForm.name.trim();
    if (!name || !dForm.faculty || !dForm.specialty || !dForm.course) return setMessage("Заполните дисциплину, курс, факультет и специальность.");
    const meta = { faculty: dForm.faculty, specialty: dForm.specialty, course: dForm.course, fullName: dForm.fullName.trim() || name };
    setDisciplineMeta((prev) => {
      const next = { ...prev, [name]: meta };
      if (dForm.editTarget && dForm.editTarget !== name) delete next[dForm.editTarget];
      return next;
    });
    setSpecialtyDisciplines((prev) => {
      const nextItem = { id: dForm.editTarget || name, name, ...meta };
      return [...prev.filter((item) => item.name !== dForm.editTarget && item.name !== name), nextItem];
    });
    const id = disciplines.find((d) => d.name === dForm.editTarget)?.id;
    const teacherId = teacherIdByName(dForm.teacherName);
    if (teacherId) {
      await saveEntity("disciplines", id, { name, teacherId }, () => {});
    }
    setDForm({ editTarget: "", name: "", fullName: "", teacherName: "", faculty: dForm.faculty, specialty: dForm.specialty, course: dForm.course });
    setMessage(teacherId ? "Дисциплина сохранена и преподаватель назначен." : "Дисциплина добавлена на специальность. Преподавателя можно назначить позже.");
  };
  const deleteDiscipline = async () => {
    const id = disciplines.find((d) => d.name === dForm.editTarget)?.id;
    if (!id) return;
    await api.remove("disciplines", id);
    await loadAll();
  };

  const saveStudent = async () => {
    const id = students.find((s) => UiUtils.fullName(s) === stForm.editTarget)?.id;
    const nextFullName = `${stForm.lastName} ${stForm.firstName} ${stForm.middleName}`.trim();
    const effectiveGroupNumber = stForm.groupNumber || "";
    const groupId = groupIdByNumber(effectiveGroupNumber);
    const groupAcademic = groupAcademicOfNumber(effectiveGroupNumber);
    const peer = students.find((s) => String(s.groupNumber) === String(effectiveGroupNumber) && UiUtils.fullName(s) !== stForm.editTarget);
    const disciplineNamesForGroup = peer?.disciplines?.length ? peer.disciplines : stForm.disciplineNames;
    const fallbackDisciplineIds = disciplines.slice(0, 2).map((d) => d.id).filter(Boolean);
    const disciplineIds = disciplineNamesForGroup.map(disciplineIdByName).filter(Boolean);
    const safeDisciplineIds = disciplineIds.length ? disciplineIds : fallbackDisciplineIds;
    await saveEntity("students", id, {
      firstName: stForm.firstName,
      lastName: stForm.lastName,
      middleName: stForm.middleName,
      groupId,
      disciplineIds: safeDisciplineIds
    }, () => setStForm({
      editTarget: "",
      firstName: "",
      lastName: "",
      middleName: "",
      groupNumber: "",
      disciplineNames: [],
      course: "",
      faculty: "",
      specialty: "",
      birthDate: "",
      studentStatus: "Активен",
      sickLeaveFrom: "",
      sickLeaveTo: "",
      curator: "",
      starosta: ""
    }));
    const fullName = `${stForm.lastName} ${stForm.firstName} ${stForm.middleName}`.trim();
    setStudentProfiles((prev) => ({
      ...prev,
      [fullName]: {
        ...(prev[stForm.editTarget] || prev[fullName] || {}),
        course: groupAcademic.course || stForm.course || "",
        faculty: groupAcademic.faculty || stForm.faculty || "",
        specialty: groupAcademic.specialty || stForm.specialty || "",
        birthDate: stForm.birthDate || prev[fullName]?.birthDate || "",
        studentStatus: stForm.studentStatus || "Активен",
        sickLeaveFrom: stForm.sickLeaveFrom || "",
        sickLeaveTo: stForm.sickLeaveTo || ""
      }
    }));
    if (stForm.editTarget && nextFullName && stForm.editTarget !== nextFullName) {
      setStudentProfiles((prev) => {
        const next = { ...prev };
        delete next[stForm.editTarget];
        return next;
      });
      setStudentFeeds((prev) => {
        const next = { ...prev, [nextFullName]: prev[stForm.editTarget] || prev[nextFullName] || [] };
        delete next[stForm.editTarget];
        return next;
      });
      setStudentQuotes((prev) => {
        const next = { ...prev, [nextFullName]: prev[stForm.editTarget] || prev[nextFullName] || [] };
        delete next[stForm.editTarget];
        return next;
      });
    }
    if (effectiveGroupNumber) {
      setGroupMeta((prev) => ({
        ...prev,
        [String(effectiveGroupNumber)]: {
          ...(prev[String(effectiveGroupNumber)] || {}),
          curator: stForm.curator || "",
          starosta: stForm.starosta || ""
        }
      }));
    }
  };
  const deleteStudent = async () => {
    const id = students.find((s) => UiUtils.fullName(s) === stForm.editTarget)?.id;
    if (!id) return;
    await api.remove("students", id);
    await loadAll();
  };

  const deleteStudentByName = async (fullName) => {
    const target = students.find((s) => UiUtils.fullName(s) === fullName);
    if (!target?.id) return;
    await api.remove("students", target.id);
    await loadAll();
    setMessage(`Студент удалён: ${fullName}`);
  };

  const bulkSetStudentStatus = async (studentNames, status) => {
    setStudentProfiles((prev) => {
      const next = { ...prev };
      studentNames.forEach((name) => {
        next[name] = {
          ...(next[name] || {}),
          studentStatus: status
        };
      });
      return next;
    });
    setMessage(`Статус "${status}" назначен (${studentNames.length}).`);
  };

  const bulkMoveStudentsToGroup = async (studentNames, targetGroupNumber) => {
    const groupId = groupIdByNumber(targetGroupNumber);
    if (!groupId) return setMessage("Группа не найдена.");
    const targetMeta = groupAcademicOfNumber(targetGroupNumber);
    for (const fullName of studentNames) {
      const student = students.find((s) => UiUtils.fullName(s) === fullName);
      if (!student) continue;
      const peer = students.find((s) => String(s.groupNumber) === String(targetGroupNumber) && UiUtils.fullName(s) !== fullName);
      const names = peer?.disciplines?.length ? peer.disciplines : student.disciplines;
      const disciplineIds = (names || []).map(disciplineIdByName).filter(Boolean);
      const safeDisciplineIds = disciplineIds.length ? disciplineIds : disciplines.slice(0, 2).map((discipline) => discipline.id).filter(Boolean);
      await api.save("students", student.id, {
        firstName: student.firstName,
        lastName: student.lastName,
        middleName: student.middleName,
        groupId,
        disciplineIds: safeDisciplineIds
      });
    }
    setStudentProfiles((prev) => {
      const next = { ...prev };
      studentNames.forEach((name) => {
        next[name] = {
          ...(next[name] || {}),
          course: targetMeta.course,
          faculty: targetMeta.faculty,
          specialty: targetMeta.specialty,
          studentStatus: next[name]?.studentStatus === "Без группы" ? "Активен" : (next[name]?.studentStatus || "Активен")
        };
      });
      return next;
    });
    await loadAll();
    setMessage(`Студенты переведены в группу ${targetGroupNumber}.`);
  };

  const bulkDeleteStudents = async (studentNames) => {
    for (const fullName of studentNames) {
      const student = students.find((s) => UiUtils.fullName(s) === fullName);
      if (student?.id) await api.remove("students", student.id);
    }
    await loadAll();
    setMessage(`Удалено студентов: ${studentNames.length}.`);
  };

  const assignStarosta = (studentName, groupNumber) => {
    if (!studentName || !groupNumber) return;
    setGroupMeta((prev) => ({
      ...prev,
      [String(groupNumber)]: {
        ...(prev[String(groupNumber)] || {}),
        starosta: studentName
      }
    }));
    setMessage(`Староста назначен: ${studentName}`);
  };

  const addFacultyToCatalog = (name, fullName = "") => {
    const value = String(name || "").trim();
    if (!value) return;
    setFacultiesCatalog((prev) => (prev.includes(value) ? prev : [...prev, value]));
    setFacultyMeta((prev) => ({
      ...prev,
      [value]: {
        ...(prev[value] || {}),
        fullName: String(fullName || prev[value]?.fullName || value).trim()
      }
    }));
    setSpecialtiesCatalog((prev) => ({ ...prev, [value]: prev[value] || [] }));
  };

  const specialtyMetaKey = (faculty, specialty) => `${faculty}__${specialty}`;

  const addSpecialtyToCatalog = (faculty, specialty, fullName = "") => {
    const f = String(faculty || "").trim();
    const s = String(specialty || "").trim();
    if (!f || !s) return;
    addFacultyToCatalog(f);
    setSpecialtyMeta((prev) => ({
      ...prev,
      [specialtyMetaKey(f, s)]: {
        ...(prev[specialtyMetaKey(f, s)] || {}),
        fullName: String(fullName || prev[specialtyMetaKey(f, s)]?.fullName || s).trim()
      }
    }));
    setSpecialtiesCatalog((prev) => {
      const list = prev[f] || [];
      return { ...prev, [f]: list.includes(s) ? list : [...list, s] };
    });
  };

  const renameFaculty = (oldName, nextName, fullName = "") => {
    const oldValue = String(oldName || "").trim();
    const nextValue = String(nextName || "").trim();
    if (!oldValue || !nextValue) return;
    if (oldValue === nextValue) {
      setFacultyMeta((prev) => ({
        ...prev,
        [oldValue]: {
          ...(prev[oldValue] || {}),
          fullName: String(fullName || prev[oldValue]?.fullName || oldValue).trim()
        }
      }));
      setEditingFaculty(null);
      setFacultyDraft("");
      setFacultyFullDraft("");
      setMessage("Факультет обновлён.");
      return;
    }
    setFacultiesCatalog((prev) => prev.map((item) => (item === oldValue ? nextValue : item)));
    setFacultyMeta((prev) => {
      const next = {
        ...prev,
        [nextValue]: {
          ...(prev[oldValue] || {}),
          fullName: String(fullName || prev[oldValue]?.fullName || nextValue).trim()
        }
      };
      delete next[oldValue];
      return next;
    });
    setSpecialtiesCatalog((prev) => {
      const next = { ...prev, [nextValue]: prev[oldValue] || [] };
      delete next[oldValue];
      return next;
    });
    setGroupMeta((prev) => Object.fromEntries(Object.entries(prev).map(([key, value]) => [key, { ...value, faculty: value.faculty === oldValue ? nextValue : value.faculty }])));
    setStudentProfiles((prev) => Object.fromEntries(Object.entries(prev).map(([key, value]) => [key, { ...value, faculty: value.faculty === oldValue ? nextValue : value.faculty }])));
    setSpecialtyDisciplines((prev) => prev.map((item) => ({ ...item, faculty: item.faculty === oldValue ? nextValue : item.faculty })));
    setDisciplineMeta((prev) => Object.fromEntries(Object.entries(prev).map(([key, value]) => [key, { ...value, faculty: value.faculty === oldValue ? nextValue : value.faculty }])));
    if (selectedAdminFaculty === oldValue) setSelectedAdminFaculty(nextValue);
    setEditingFaculty(null);
    setFacultyDraft("");
    setFacultyFullDraft("");
    setMessage("Факультет обновлён.");
  };

  const deleteFacultyFromCatalog = (faculty) => {
    setFacultiesCatalog((prev) => prev.filter((item) => item !== faculty));
    setFacultyMeta((prev) => {
      const next = { ...prev };
      delete next[faculty];
      return next;
    });
    setSpecialtiesCatalog((prev) => {
      const next = { ...prev };
      delete next[faculty];
      return next;
    });
    if (selectedAdminFaculty === faculty) setSelectedAdminFaculty(facultiesCatalog.find((item) => item !== faculty) || "");
    setPendingFacultyDelete(null);
    setMessage("Факультет удалён из каталога. Связанные группы и студенты сохранены.");
  };

  const renameSpecialty = (faculty, oldName, nextName, fullName = "") => {
    const oldValue = String(oldName || "").trim();
    const nextValue = String(nextName || "").trim();
    if (!faculty || !oldValue || !nextValue) return;
    if (oldValue === nextValue) {
      setSpecialtyMeta((prev) => ({
        ...prev,
        [specialtyMetaKey(faculty, oldValue)]: {
          ...(prev[specialtyMetaKey(faculty, oldValue)] || {}),
          fullName: String(fullName || prev[specialtyMetaKey(faculty, oldValue)]?.fullName || oldValue).trim()
        }
      }));
      setEditingSpecialty(null);
      setSpecialtyDraft("");
      setSpecialtyFullDraft("");
      setMessage("Специальность обновлена.");
      return;
    }
    setSpecialtiesCatalog((prev) => ({
      ...prev,
      [faculty]: (prev[faculty] || []).map((item) => (item === oldValue ? nextValue : item))
    }));
    setSpecialtyMeta((prev) => {
      const oldKey = specialtyMetaKey(faculty, oldValue);
      const nextKey = specialtyMetaKey(faculty, nextValue);
      const next = {
        ...prev,
        [nextKey]: {
          ...(prev[oldKey] || {}),
          fullName: String(fullName || prev[oldKey]?.fullName || nextValue).trim()
        }
      };
      delete next[oldKey];
      return next;
    });
    setGroupMeta((prev) => Object.fromEntries(Object.entries(prev).map(([key, value]) => [key, { ...value, specialty: value.faculty === faculty && value.specialty === oldValue ? nextValue : value.specialty }])));
    setStudentProfiles((prev) => Object.fromEntries(Object.entries(prev).map(([key, value]) => [key, { ...value, specialty: value.faculty === faculty && value.specialty === oldValue ? nextValue : value.specialty }])));
    setSpecialtyDisciplines((prev) => prev.map((item) => ({ ...item, specialty: item.faculty === faculty && item.specialty === oldValue ? nextValue : item.specialty })));
    setDisciplineMeta((prev) => Object.fromEntries(Object.entries(prev).map(([key, value]) => [key, { ...value, specialty: value.faculty === faculty && value.specialty === oldValue ? nextValue : value.specialty }])));
    setEditingSpecialty(null);
    setSpecialtyDraft("");
    setSpecialtyFullDraft("");
    setMessage("Специальность обновлена.");
  };

  const deleteSpecialtyFromCatalog = (faculty, specialty) => {
    setSpecialtiesCatalog((prev) => ({
      ...prev,
      [faculty]: (prev[faculty] || []).filter((item) => item !== specialty)
    }));
    setSpecialtyMeta((prev) => {
      const next = { ...prev };
      delete next[specialtyMetaKey(faculty, specialty)];
      return next;
    });
    setPendingSpecialtyDelete(null);
    setMessage("Специальность удалена из каталога. Связанные группы и студенты сохранены.");
  };

  const assignTeacherDiscipline = async () => {
    if (!teacherDisciplineModal?.teacherName || !teacherDisciplineModal?.disciplineName) return;
    const teacherId = teacherIdByName(teacherDisciplineModal.teacherName);
    if (!teacherId) return setMessage("Преподаватель не найден.");
    const disciplineName = teacherDisciplineModal.disciplineName.trim();
    const existing = disciplines.find((d) => d.name === disciplineName);
    await saveEntity("disciplines", existing?.id, { name: disciplineName, teacherId }, () => {});
    setDisciplineMeta((prev) => ({
      ...prev,
      [disciplineName]: {
        ...(prev[disciplineName] || {}),
        faculty: teacherDisciplineModal.faculty,
        specialty: teacherDisciplineModal.specialty,
        course: teacherDisciplineModal.course || "1 курс"
      }
    }));
    setTeacherDisciplineModal(null);
    setMessage("Дисциплина назначена преподавателю.");
  };

  const importStudentsBatch = async (items) => {
    let createdOrUpdated = 0;
    const fallbackDisciplineIds = disciplines.slice(0, 2).map((d) => d.id).filter(Boolean);
    for (const row of items) {
      const fullNameRaw = String(row.fullName || "").trim();
      const parts = fullNameRaw.split(/\s+/).filter(Boolean);
      const lastName = String(row.lastName || parts[0] || "").trim();
      const firstName = String(row.firstName || parts[1] || "").trim();
      const middleName = String(row.middleName || parts.slice(2).join(" ") || "").trim();
      if (!lastName || !firstName) continue;
      const fullName = `${lastName} ${firstName} ${middleName}`.trim();
      const existing = students.find((s) => UiUtils.fullName(s) === fullName);
      const groupId = existing?.groupNumber ? groupIdByNumber(existing.groupNumber) : null;
      const disciplineIds = (existing?.disciplines || []).map(disciplineIdByName).filter(Boolean);
      const safeDisciplineIds = disciplineIds.length ? disciplineIds : fallbackDisciplineIds;
      await api.save("students", existing?.id, { firstName, lastName, middleName, groupId, disciplineIds: safeDisciplineIds });
      createdOrUpdated += 1;
    }
    await loadAll();
    setMessage(`Импорт завершен. Обработано записей: ${createdOrUpdated}.`);
  };

  const uploadAvatar = async (student, file) => {
    const data = await UiUtils.toDataUrl(file);
    setAvatars((prev) => ({ ...prev, [student.id]: data }));
  };

  const parseFullName = (fullName) => {
    const parts = (fullName || "").trim().split(/\s+/);
    return {
      lastName: parts[0] || "",
      firstName: parts[1] || "",
      middleName: parts.slice(2).join(" ") || ""
    };
  };

  const isStudentOnSickLeaveOnDate = (studentName, rawDate = new Date().toISOString().slice(0, 10)) => {
    const meta = studentProfiles[studentName] || {};
    if (meta.studentStatus !== "На больничном") return false;
    const from = meta.sickLeaveFrom ? new Date(`${meta.sickLeaveFrom}T00:00:00`) : null;
    const to = meta.sickLeaveTo ? new Date(`${meta.sickLeaveTo}T23:59:59`) : null;
    const now = new Date(`${rawDate}T12:00:00`);
    if (from && Number.isNaN(from.getTime())) return true;
    if (to && Number.isNaN(to.getTime())) return true;
    if (from && now < from) return false;
    if (to && now > to) return false;
    return true;
  };
  const grantTeacherAccess = async (email) => {
    const account = userAccounts.find((a) => normalizeEmail(a.email) === normalizeEmail(email));
    if (!account) return setMessage("Аккаунт не найден.");
    if (teacherAccessEmails.some((m) => m.toLowerCase() === email.toLowerCase())) return setMessage("Доступ уже выдан.");
    const isTeacherInBase = teachers.some((t) => UiUtils.fullName(t).toLowerCase() === account.fullName.toLowerCase());
    if (!isTeacherInBase) {
      const split = parseFullName(account.fullName);
      await saveEntity("teachers", null, split, () => {});
    }
    setTeacherAccessEmails((prev) => [...prev, account.email]);
    await loadAll();
    setMessage(`Пользователь назначен преподавателем: ${account.fullName}`);
  };

  const revokeTeacherAccess = (email) => {
    setTeacherAccessEmails((prev) => prev.filter((m) => m.toLowerCase() !== email.toLowerCase()));
    setMessage("Доступ преподавателя отозван.");
  };

  const noteOwnerName = session?.role === "student" ? session.studentName : (session?.role === "teacher" ? session.teacherName : "");

  const publishPost = () => {
    const text = (newNoteText || feedDraft).trim();
    if (!text || !noteOwnerName) return;
    const key = noteOwnerName;
    const post = { id: Date.now(), text, createdAt: new Date().toLocaleString("ru-RU") };
    setStudentFeeds((prev) => ({ ...prev, [key]: [post, ...(prev[key] || [])] }));
    setFeedDraft("");
    setNewNoteText("");
    setShowNewNoteModal(false);
  };

  const removePost = (id) => {
    if (!noteOwnerName) return;
    if (!window.confirm("Точно удалить заметку?")) return;
    const key = noteOwnerName;
    setStudentFeeds((prev) => ({ ...prev, [key]: (prev[key] || []).filter((post) => post.id !== id) }));
    setEditingPostId(null);
    setEditingPostText("");
    setOpenPostMenuId(null);
  };

  const startEditPost = (post) => {
    setEditingPostId(post.id);
    setEditingPostText(post.text);
  };

  const saveEditPost = (id) => {
    if (!noteOwnerName) return;
    const key = noteOwnerName;
    const text = editingPostText.trim();
    if (!text) return;
    setStudentFeeds((prev) => ({
      ...prev,
      [key]: (prev[key] || []).map((post) => (post.id === id ? { ...post, text } : post))
    }));
    setEditingPostId(null);
    setEditingPostText("");
    setOpenPostMenuId(null);
  };

  const addTeacherLesson = () => {
    if (!teacherLessonForm.discipline || !teacherLessonForm.group || !session?.teacherName) return;
    const slot = lessonSlots.find((s) => s.number === Number(teacherLessonForm.slot));
    const hasRoomConflict = fullSchedule.some((lesson) => (
      lesson.id !== teacherLessonForm.editId
      && lessonKey(lesson) !== teacherLessonForm.replacesKey
      && lesson.day === teacherLessonForm.day
      && Number(lesson.slot) === Number(teacherLessonForm.slot)
      && String(lesson.room || "").trim().toLowerCase() === String(teacherLessonForm.room || "").trim().toLowerCase()
    ));
    if (hasRoomConflict) {
      setMessage("Конфликт аудитории: выберите другую аудиторию.");
      return;
    }
    const lesson = {
      id: teacherLessonForm.editId || nextLocalId(),
      replacesKey: teacherLessonForm.replacesKey || "",
      day: teacherLessonForm.day,
      slot: Number(teacherLessonForm.slot),
      time: slot ? `${slot.start} - ${slot.end}` : "",
      lessonType: normalizeLessonType(teacherLessonForm.lessonType),
      discipline: teacherLessonForm.discipline,
      teacher: session.teacherName,
      group: teacherLessonForm.group,
      room: teacherLessonForm.room || "500-к."
    };
    setCustomLessons((prev) => [...prev.filter((item) => item.id !== lesson.id), lesson]);
    setMessage(teacherLessonForm.editId ? "Пара изменена." : "Пара добавлена.");
  };

  const fillTeacherLessonForm = (lesson) => {
    setTeacherLessonForm({
      editId: lesson.id || null,
      replacesKey: lesson.id ? (lesson.replacesKey || "") : lessonKey(lesson),
      day: lesson.day,
      slot: Number(lesson.slot),
      discipline: lesson.discipline || "",
      group: lesson.group || "",
      room: lesson.room || "500-к.",
      lessonType: normalizeLessonType(lesson.lessonType)
    });
  };

  const startTeacherLessonAt = (day, slotNumber) => {
    setTeacherLessonForm((prev) => ({
      ...prev,
      editId: null,
      replacesKey: "",
      day,
      slot: Number(slotNumber),
      discipline: "",
      group: "",
      room: "500-к.",
      lessonType: "ЛК"
    }));
  };

  const deleteTeacherLesson = () => {
    if (teacherLessonForm.editId) {
      setCustomLessons((prev) => prev.filter((lesson) => lesson.id !== teacherLessonForm.editId));
    } else if (teacherLessonForm.replacesKey) {
      setCustomLessons((prev) => [...prev, {
        id: nextLocalId(),
        replacesKey: teacherLessonForm.replacesKey,
        day: teacherLessonForm.day,
        slot: Number(teacherLessonForm.slot),
        teacher: session.teacherName,
        group: teacherLessonForm.group,
        cancelled: true
      }]);
    } else {
      return;
    }
    setTeacherLessonForm({ editId: null, replacesKey: "", day: "Понедельник", slot: 1, discipline: "", group: "", room: "500-к.", lessonType: "ЛК" });
    setMessage("Пара удалена.");
  };

  const addAdminLesson = () => {
    if (!adminLessonForm.discipline || !adminLessonForm.group || !adminLessonForm.teacher) return;
    const slot = lessonSlots.find((s) => s.number === Number(adminLessonForm.slot));
    const conflictingLesson = fullSchedule.find((lesson) => (
      lesson.id !== adminLessonForm.editId
      && lessonKey(lesson) !== adminLessonForm.replacesKey
      && lesson.day === adminLessonForm.day
      && Number(lesson.slot) === Number(adminLessonForm.slot)
      && (
        String(lesson.room || "").trim().toLowerCase() === String(adminLessonForm.room || "").trim().toLowerCase()
        || String(lesson.group || "") === String(adminLessonForm.group || "")
        || String(lesson.teacher || "") === String(adminLessonForm.teacher || "")
      )
    ));
    if (conflictingLesson) {
      const sameRoom = String(conflictingLesson.room || "").trim().toLowerCase() === String(adminLessonForm.room || "").trim().toLowerCase();
      const sameGroup = String(conflictingLesson.group || "") === String(adminLessonForm.group || "");
      setMessage(sameRoom
        ? "Конфликт аудитории: выберите другую аудиторию."
        : (sameGroup ? "Конфликт группы: у группы уже есть пара в это время." : "Конфликт преподавателя: преподаватель уже занят в это время."));
      return;
    }
    const lesson = {
      id: adminLessonForm.editId || nextLocalId(),
      replacesKey: adminLessonForm.replacesKey || "",
      day: adminLessonForm.day,
      slot: Number(adminLessonForm.slot),
      time: slot ? `${slot.start} - ${slot.end}` : "",
      lessonType: normalizeLessonType(adminLessonForm.lessonType),
      discipline: adminLessonForm.discipline,
      teacher: adminLessonForm.teacher,
      group: adminLessonForm.group,
      room: adminLessonForm.room || "500-к."
    };
    setCustomLessons((prev) => [...prev.filter((item) => item.id !== lesson.id), lesson]);
    logAdminAction("Расписание", `${adminLessonForm.editId ? "Изменена" : "Добавлена"} пара: ${lesson.discipline}, ${lesson.group}, ${lesson.time}`);
    setSelectedAdminLessonId(`custom-${lesson.id}`);
    setAdminScheduleFilters((prev) => ({ ...prev, group: lesson.group }));
    setMessage(adminLessonForm.editId ? "Пара изменена." : "Пара добавлена.");
  };

  const removeScheduleLesson = () => {
    if (adminLessonForm.editId) {
      setCustomLessons((prev) => {
        const withoutCurrent = prev.filter((it) => it.id !== adminLessonForm.editId);
        if (!adminLessonForm.replacesKey) return withoutCurrent;
        return [
          ...withoutCurrent,
          {
            id: nextLocalId(),
            replacesKey: adminLessonForm.replacesKey,
            day: adminLessonForm.day,
            slot: Number(adminLessonForm.slot),
            group: adminLessonForm.group,
            teacher: adminLessonForm.teacher,
            cancelled: true
          }
        ];
      });
      logAdminAction("Расписание", `Удалена пара #${adminLessonForm.editId}`);
    } else if (adminLessonForm.replacesKey) {
      const tombstone = {
        id: nextLocalId(),
        replacesKey: adminLessonForm.replacesKey,
        day: adminLessonForm.day,
        slot: Number(adminLessonForm.slot),
        group: adminLessonForm.group,
        teacher: adminLessonForm.teacher,
        cancelled: true
      };
      setCustomLessons((prev) => [...prev, tombstone]);
      logAdminAction("Расписание", `Удалена базовая пара: ${adminLessonForm.replacesKey}`);
    } else {
      return;
    }
    setSelectedAdminLessonId(null);
    setAdminLessonForm({ editId: null, replacesKey: "", day: "Понедельник", slot: 1, discipline: "", group: "", room: "500-к.", teacher: "", lessonType: "ЛК" });
    setMessage("Пара удалена.");
  };

  const saveTeacherJournalRow = async (studentName, rawValueOverride) => {
    const disciplineName = teacherJournalContext.disciplineName;
    if (!studentName || !disciplineName) {
      setMessage("Сначала выберите группу и предмет.");
      return;
    }
    if (teacherJournalSavingRef.current[studentName]) return;
    teacherJournalSavingRef.current[studentName] = true;
    const rawValue = rawValueOverride ?? teacherJournalDrafts[studentName] ?? "";
    const isAbsenceMark = String(rawValue || "").trim().toUpperCase() === "Н";
    const lessonType = normalizeLessonType(teacherJournalContext.lessonType);
    const date = teacherJournalContext.date || new Date().toISOString().slice(0, 10);

    try {
      if (teacherJournalMode === "grade" && !isAbsenceMark) {
        const currentGrades = gradesForJournalCell({ studentName, disciplineName, lessonType, date });
        if (String(rawValue ?? "").trim() === "") {
          await deleteJournalGrades(currentGrades);
          await loadAll();
          setTeacherJournalDrafts((prev) => {
            const next = { ...prev };
            delete next[studentName];
            return next;
          });
          setMessage("Оценки удалены.");
          return;
        }
        if (!isValidGradeInput(rawValue)) {
          setMessage("Оценки должны быть от 0 до 10. Несколько оценок вводите через запятую.");
          return;
        }
        if (hasAbsenceOnDate({ studentName, disciplineName, lessonType, date })) {
          setMessage("На эту дату уже стоит пропуск. Оценку поставить нельзя.");
          return;
        }
        const studentId = studentIdByName(studentName);
        const disciplineId = disciplineIdByName(disciplineName);
        if (!studentId || !disciplineId) {
          setMessage("Не удалось определить студента или предмет.");
          return;
        }
        await deleteJournalGrades(currentGrades);
        const savedGrades = [];
        for (const value of gradeNumbers(rawValue)) {
          const savedGrade = await api.save("grades", null, { value, studentId, disciplineId });
          savedGrades.push(savedGrade);
        }
        setGradeLessonMeta((prev) => {
          const next = { ...prev };
          savedGrades.forEach((savedGrade, index) => {
            next[savedGrade?.id ? `grade:${savedGrade.id}` : `${gradeMetaKey(studentName, disciplineName)}__${date}__${lessonType}__${index}`] = {
              lessonType,
              teacherName: session.teacherName,
              date,
              updatedAt: new Date().toLocaleString("ru-RU"),
              author: session.teacherName
            };
          });
          return next;
        });
        await loadAll();
        setMessage(`Оценка сохранена (${new Date(`${date}T12:00:00`).toLocaleDateString("ru-RU")}).`);
        setTeacherJournalDrafts((prev) => {
          const next = { ...prev };
          delete next[studentName];
          return next;
        });
        return;
      }

      if (String(rawValue ?? "").trim() === "") {
        setAbsences((prev) => prev.filter((absence) => !(
          absence.studentName === studentName
          && absence.disciplineName === disciplineName
          && normalizeLessonType(absence.lessonType) === lessonType
          && absence.date === date
        )));
        setTeacherJournalDrafts((prev) => ({ ...prev, [studentName]: "" }));
        setMessage("Пропуски удалены.");
        return;
      }
      const numericValue = isAbsenceMark ? ACADEMIC_HOURS_PER_ABSENCE : Number(rawValue);
      if (Number.isNaN(numericValue) || numericValue < 0) {
        setMessage("Введите корректное значение пропуска.");
        return;
      }
      const absenceUnits = Math.max(0, Math.round(numericValue / ACADEMIC_HOURS_PER_ABSENCE));
      const isSick = isStudentOnSickLeaveOnDate(studentName, date);
      const record = {
        id: nextLocalId(),
        studentName,
        disciplineName,
        count: isSick ? 0 : absenceUnits,
        lessonType,
        teacherName: session.teacherName,
        date,
        reason: isSick ? "Больничный" : "",
        excusedHours: isSick
          ? absenceUnits * ACADEMIC_HOURS_PER_ABSENCE
          : 0,
        author: session.teacherName,
        updatedAt: new Date().toLocaleString("ru-RU")
      };
      setAbsences((prev) => [record, ...prev]);
      setTeacherJournalDrafts((prev) => ({ ...prev, [studentName]: "" }));
      setMessage(isSick
        ? "Студент на больничном: часы отмечены как уважительная причина и не попали в пропуски."
        : `Пропуски сохранены (${new Date(`${date}T12:00:00`).toLocaleDateString("ru-RU")}).`);
    } finally {
      teacherJournalSavingRef.current[studentName] = false;
    }
  };

  const openTeacherProfileEditor = () => {
    if (!session?.teacherName) return;
    setTeacherProfileDraft({ ...teacherProfileOf(session.teacherName) });
    setTeacherHomeEditOpen(true);
  };

  const updateTeacherDraftField = (field, value) => {
    setTeacherProfileDraft((prev) => ({ ...(prev || {}), [field]: value }));
  };

  const closeTeacherProfileEditor = () => {
    setTeacherHomeEditOpen(false);
    setTeacherProfileDraft(null);
  };

  const updateTeacherDraftAvatar = async (file) => {
    if (!file) return;
    const data = await UiUtils.toDataUrl(file);
    setTeacherProfileDraft((prev) => ({ ...(prev || {}), avatar: data }));
  };

  const saveTeacherProfileDraft = () => {
    if (!session?.teacherName || !teacherProfileDraft) return;
    setTeacherProfiles((prev) => ({
      ...prev,
      [session.teacherName]: {
        ...teacherProfileOf(session.teacherName),
        ...teacherProfileDraft
      }
    }));
    setMessage("Профиль преподавателя сохранен.");
    closeTeacherProfileEditor();
  };

  const studentMetaOf = (name) => studentProfiles[name] || {};
  const groupAcademicOfNumber = (number) => {
    const group = groups.find((g) => String(g.number) === String(number));
    const saved = groupMeta[String(number)] || {};
    const firstStudentName = group?.students?.[0];
    const firstMeta = firstStudentName ? studentMetaOf(firstStudentName) : {};
    const groupFaculty = group?.faculty || saved.faculty || firstMeta.faculty || facultiesCatalog[0] || "";
    return {
      course: group?.course || saved.course || firstMeta.course || "1 курс",
      faculty: groupFaculty,
      specialty: group?.specialty || saved.specialty || firstMeta.specialty || specialtiesCatalog[groupFaculty]?.[0] || ""
    };
  };

  const myStudent = session?.role === "student"
    ? students.find((s) => normalizeFullName(UiUtils.fullName(s)) === normalizeFullName(session.studentName))
    : null;
  const myGrades = session?.role === "student" ? grades.filter((g) => g.studentName === session.studentName) : [];
  const mySchedule = session?.role === "student" && myStudent
    ? fullSchedule.filter((it) => String(it.group) === String(myStudent.groupNumber))
    : [];
  const myAbsences = myStudent
    ? absences.filter((a) => a.studentName === UiUtils.fullName(myStudent))
    : [];
  const myAvg = session?.role === "student" ? (avgMap[session.studentName] || "нет") : "нет";
  const myGroup = session?.role === "student" && myStudent
    ? groups.find((g) => String(g.number) === String(myStudent.groupNumber))
    : null;
  const myGroupMembers = [...(myGroup?.students || [])].sort((a, b) => String(a).localeCompare(String(b), "ru"));
  const selectedAdminGroupMembers = groups.find((g) => String(g.number) === String(stForm.groupNumber))?.students || [];
  const groupSettings = myStudent?.groupNumber ? (groupMeta[String(myStudent.groupNumber)] || {}) : {};
  const groupCurator = groupSettings.curator || mySchedule[0]?.teacher || "не назначен";
  const starostaName = groupSettings.starosta || "";
  const myFeed = noteOwnerName ? (studentFeeds[noteOwnerName] || []) : [];
  const profileData = session?.role === "student"
    ? (studentProfiles[session.studentName] || {})
    : {};
  const currentAccount = useMemo(
    () => userAccounts.find((a) => normalizeEmail(a.email) === normalizeEmail(session?.email)),
    [userAccounts, session]
  );
  const scheduleDays = UiUtils.days.slice(0, 6);
  const myJournal = useMemo(() => {
    if (!myStudent) return { cards: [], summary: { avg: "—", best: "—", worst: "—", totalAbsences: 0 } };
    const groupSubjects = [...new Set(mySchedule.map((it) => it.discipline))];
    const subjects = [...new Set([...groupSubjects, ...myGrades.map((g) => g.disciplineName)])].sort((a, b) => String(a).localeCompare(String(b), "ru"));
    const cards = subjects.map((subject, subjectIdx) => {
      const subjectLessons = mySchedule.filter((lesson) => lesson.discipline === subject);
      const subjectGrades = myGrades.filter((grade) => grade.disciplineName === subject);
      const subjectAbsences = myAbsences.filter((absence) => absence.disciplineName === subject);
      const teacherName = subjectLessons[0]?.teacher || disciplines.find((d) => d.name === subject)?.teacherName || "—";
      const typeCodes = [...new Set([
        ...subjectLessons.map((lesson, idx) => normalizeLessonType(lesson.lessonType, idx)),
        ...LESSON_TYPES.map((type) => type.code)
      ])];
      const groupsByType = typeCodes.map((typeCode, typeIdx) => {
        const typeLessons = subjectLessons.filter((lesson, idx) => normalizeLessonType(lesson.lessonType, idx) === typeCode);
        const entries = subjectGrades
          .filter((grade) => normalizeLessonType(grade.lessonType || gradeMetaOf(grade).lessonType, subjectIdx + typeIdx) === typeCode)
          .map((grade, gradeIdx) => {
            const meta = gradeMetaOf(grade);
            const fallback = new Date(Date.now() - (subjectIdx * 5 + gradeIdx) * 86400000);
            const rawDate = grade.date || meta.date;
            const dateLabel = rawDate ? new Date(rawDate).toLocaleDateString("ru-RU") : fallback.toLocaleDateString("ru-RU");
            return {
              ...grade,
              dateLabel,
              teacherName: meta.teacherName || typeLessons[0]?.teacher || teacherName,
              typeMeta: lessonTypeMeta(typeCode)
            };
          });
        return {
          lessonType: typeCode,
          typeMeta: lessonTypeMeta(typeCode),
          teacherName: typeLessons[0]?.teacher || teacherName,
          grades: entries,
          avg: averageValue(entries, (entry) => entry.value)
        };
      });
      const avg = averageValue(subjectGrades, (grade) => grade.value);
      return {
        subject,
        avg,
        absences: subjectAbsences.reduce((sum, item) => sum + Number(item.count || 0), 0) * ACADEMIC_HOURS_PER_ABSENCE,
        groups: groupsByType
      };
    });
    const ranked = cards.filter((card) => card.avg !== "—").sort((a, b) => Number(b.avg) - Number(a.avg));
    return {
      cards: cards.sort((a, b) => String(a.subject).localeCompare(String(b.subject), "ru")),
      summary: {
        avg: averageValue(myGrades, (grade) => grade.value),
        best: ranked[0]?.subject || "—",
        worst: ranked[ranked.length - 1]?.subject || "—",
        totalAbsences: myAbsences.reduce((sum, item) => sum + Number(item.count || 0), 0) * ACADEMIC_HOURS_PER_ABSENCE
      }
    };
  }, [myGrades, myStudent, mySchedule, myAbsences, disciplines, gradeLessonMeta]);

  const filteredJournalCards = useMemo(() => myJournal.cards
    .filter((card) => !studentGradeFilters.subject || card.subject === studentGradeFilters.subject)
    .map((card) => ({
      ...card,
      groups: card.groups.filter((group) => !studentGradeFilters.lessonType || group.lessonType === studentGradeFilters.lessonType)
    }))
    .filter((card) => card.groups.length > 0), [myJournal.cards, studentGradeFilters.subject, studentGradeFilters.lessonType]);

  const searchableSchedule = useMemo(
    () => scheduleDays.map((day) => ({
      day,
      lessons: fullSchedule.filter((it) => {
        const byDay = it.day === day;
        const search = scheduleSearch.trim().toLowerCase();
        const defaultGroup = String(myStudent?.groupNumber || "");
        const byDefaultGroup = defaultGroup ? String(it.group) === defaultGroup : false;
        const bySearch = search
          ? `${it.teacher} ${it.group} ${it.discipline} ${it.lessonType}`.toLowerCase().includes(search)
          : true;
        return byDay && byDefaultGroup && bySearch;
      })
    })),
    [fullSchedule, scheduleSearch, myStudent]
  );
  const visibleStudentLessonSlots = useMemo(() => lessonSlots.filter((slot) => (
    searchableSchedule.some((block) => block.lessons.some((lesson) => Number(lesson.slot) === Number(slot.number)))
  )), [lessonSlots, searchableSchedule]);
  const disciplineMap = {
    "АПК": "Архитектура персональных компьютеров",
    "ПНаяву": "Программирование на языках высокого уровня",
    "ОИнфБ": "Основы информационной безопасности"
  };
  const disciplineFullName = (shortName) => disciplineMeta[shortName]?.fullName || disciplineMap[shortName] || shortName;
  const facultyFullName = (shortName) => facultyMeta[shortName]?.fullName || shortName;
  const specialtyFullName = (faculty, shortName) => specialtyMeta[specialtyMetaKey(faculty, shortName)]?.fullName || shortName;
  const ShortWithTooltip = ({ value, full }) => <span className="short-name-tip" title={full || value}>{value || "—"}</span>;

  const currentStudentAcademic = myStudent?.groupNumber ? groupAcademicOfNumber(myStudent.groupNumber) : {};
  const profileCourse = formatCourseDisplay(profileData.course || currentStudentAcademic.course);
  const profileFaculty = profileData.faculty || currentStudentAcademic.faculty || "Факультет не указан";
  const profileSpecialty = profileData.specialty || currentStudentAcademic.specialty || "Специальность не указана";
  useEffect(() => {
    if (session?.role !== "student" || !session.studentName || !myStudent?.groupNumber) return;
    const academic = groupAcademicOfNumber(myStudent.groupNumber);
    setStudentProfiles((prev) => ({
      ...prev,
      [session.studentName]: {
        ...(prev[session.studentName] || {}),
        course: academic.course || prev[session.studentName]?.course || "",
        faculty: academic.faculty || prev[session.studentName]?.faculty || "",
        specialty: academic.specialty || prev[session.studentName]?.specialty || ""
      }
    }));
  }, [session, myStudent?.groupNumber, groups, groupMeta]);
  const teacherDisciplines = session?.role === "teacher" ? disciplines.filter((d) => d.teacherName === session.teacherName) : [];
  const teacherSchedule = session?.role === "teacher"
    ? fullSchedule.filter((it) => it.teacher === session.teacherName)
    : [];
  const searchableTeacherScheduleByDay = useMemo(() => {
    const q = scheduleSearch.trim().toLowerCase();
    return scheduleDays.map((day) => ({
      day,
      lessons: teacherSchedule.filter((lesson) => {
        if (lesson.day !== day) return false;
        if (!q) return true;
        return `${lesson.teacher} ${lesson.group} ${lesson.discipline} ${lesson.room} ${lesson.lessonType}`.toLowerCase().includes(q);
      })
    }));
  }, [scheduleDays, teacherSchedule, scheduleSearch]);
  const teacherStudents = session?.role === "teacher"
    ? students.filter((s) => (s.disciplines || []).some((name) => teacherDisciplines.some((d) => d.name === name)))
    : [];
  const teacherGroups = [...new Set(teacherSchedule.map((it) => String(it.group || "")).filter(Boolean))];
  const teacherProfileOf = (name) => ({
    degree: "",
    position: "",
    department: "",
    email: "",
    workPhone: "",
    office: "",
    links: "",
    courses: "",
    experienceYears: "",
    workStatus: "Активен",
    ...(teacherProfiles[name] || {})
  });
  const activeTeacherProfile = session?.role === "teacher" ? teacherProfileOf(session.teacherName) : null;
  const previewTeacherProfile = teacherPreviewName ? teacherProfileOf(teacherPreviewName) : null;
  const groupRows = useMemo(() => groups.map((group) => {
    const meta = groupAcademicOfNumber(group.number);
    return {
      ...group,
      course: meta.course,
      faculty: meta.faculty,
      specialty: meta.specialty,
      studentsCount: (group.students || []).length
    };
  }), [groups, groupMeta, studentProfiles, facultiesCatalog, specialtiesCatalog]);
  const facultyStats = useMemo(() => facultiesCatalog.map((faculty) => {
    const specialties = specialtiesCatalog[faculty] || [];
    const relatedGroups = groupRows.filter((group) => group.faculty === faculty);
    const relatedStudents = students.filter((student) => {
      const meta = studentMetaOf(UiUtils.fullName(student));
      return meta.faculty === faculty || relatedGroups.some((group) => String(group.number) === String(student.groupNumber));
    });
    return { faculty, specialtiesCount: specialties.length, groupsCount: relatedGroups.length, studentsCount: relatedStudents.length };
  }), [facultiesCatalog, specialtiesCatalog, groupRows, students, studentProfiles]);
  const specialtyStatsForFaculty = (faculty, specialty) => {
    const relatedGroups = groupRows.filter((group) => group.faculty === faculty && group.specialty === specialty);
    const relatedStudents = students.filter((student) => {
      const meta = studentMetaOf(UiUtils.fullName(student));
      return meta.faculty === faculty && meta.specialty === specialty;
    });
    return { groupsCount: relatedGroups.length, studentsCount: relatedStudents.length };
  };
  const disciplineRows = useMemo(() => {
    const byName = new Map();
    specialtyDisciplines.forEach((item) => byName.set(item.name, { ...item, teacherName: "", source: "local" }));
    disciplines.forEach((item) => {
      const meta = disciplineMeta[item.name] || byName.get(item.name) || {};
      byName.set(item.name, { ...meta, ...item, source: "api" });
    });
    return [...byName.values()].map((item) => ({
      ...item,
      faculty: item.faculty || "ФКСиС",
      specialty: item.specialty || "КИ (ВМСиС)",
      course: item.course || "1 курс"
    }));
  }, [specialtyDisciplines, disciplines, disciplineMeta]);
  const sortedStudents = [...students].sort((a, b) => UiUtils.fullName(a).localeCompare(UiUtils.fullName(b), "ru"));
  const allFaculties = [...new Set(sortedStudents.map((s) => studentMetaOf(UiUtils.fullName(s)).faculty).filter(Boolean))];
  const specialtiesForFaculty = (faculty) => [...new Set(
    sortedStudents
      .filter((s) => !faculty || studentMetaOf(UiUtils.fullName(s)).faculty === faculty)
      .map((s) => studentMetaOf(UiUtils.fullName(s)).specialty)
      .filter(Boolean)
  )];
  const groupsForFacultySpecialty = (faculty, specialty) => [...new Set(
    sortedStudents
      .filter((s) => (!faculty || studentMetaOf(UiUtils.fullName(s)).faculty === faculty) && (!specialty || studentMetaOf(UiUtils.fullName(s)).specialty === specialty))
      .map((s) => String(s.groupNumber || ""))
      .filter(Boolean)
  )].sort((a, b) => a.localeCompare(b, "ru"));
  const applyHierarchyFilter = (list, faculty, specialty, group) => list.filter((s) => {
    const name = UiUtils.fullName(s);
    const meta = studentMetaOf(name);
    return (!faculty || meta.faculty === faculty)
      && (!specialty || meta.specialty === specialty)
      && (!group || String(s.groupNumber) === String(group));
  });
  const PAGE_SIZE = 8;
  const paginate = (arr, page) => arr.slice((page - 1) * PAGE_SIZE, page * PAGE_SIZE);
  const adminAllStudents = sortedStudents;
  const adminGroupedStudents = applyHierarchyFilter(sortedStudents, adminFacultyFilter, adminSpecialtyFilter, adminGroupFilter);
  const logAdminAction = (action, details) => {
    setAdminAuditLog((prev) => [{
      id: Date.now(),
      action,
      details,
      actor: "Администратор",
      createdAt: new Date().toLocaleString("ru-RU")
    }, ...prev].slice(0, 50));
  };
  const adminGroupsPreview = groups.slice(0, 6).map((g, idx) => {
    const members = g.students || [];
    const groupGrades = grades.filter((gr) => members.includes(gr.studentName));
    const avg = averageValue(groupGrades, (item) => item.value);
    const curator = (groupMeta[String(g.number)]?.curator || "—");
    return { id: g.id || idx, name: g.number, students: members.length, curator, avg };
  });
  const adminPerf = useMemo(() => {
    const bucket = { excellent: 0, good: 0, satisfactory: 0, poor: 0 };
    grades.forEach((g) => {
      gradeNumbers(g.value).forEach((v) => {
        if (v >= 9) bucket.excellent += 1;
        else if (v >= 7) bucket.good += 1;
        else if (v >= 5) bucket.satisfactory += 1;
        else bucket.poor += 1;
      });
    });
    const total = Math.max(1, bucket.excellent + bucket.good + bucket.satisfactory + bucket.poor);
    return {
      ...bucket,
      excellentPct: Math.round((bucket.excellent / total) * 100),
      goodPct: Math.round((bucket.good / total) * 100),
      satisfactoryPct: Math.round((bucket.satisfactory / total) * 100),
      poorPct: Math.round((bucket.poor / total) * 100)
    };
  }, [grades]);
  const adminActivityDonut = useMemo(() => {
    const studentsCount = students.length;
    const teachersCount = teachers.length;
    const adminsCount = 1;
    const total = Math.max(1, studentsCount + teachersCount + adminsCount);
    const s = (studentsCount / total) * 360;
    const t = (teachersCount / total) * 360;
    return {
      total,
      studentsCount,
      teachersCount,
      adminsCount,
      bg: `conic-gradient(#7e57c2 0deg ${s}deg, #ec407a ${s}deg ${s + t}deg, #ffb74d ${s + t}deg 360deg)`
    };
  }, [students.length, teachers.length]);
  const adminJournalInsights = useMemo(() => {
    const groupByStudent = Object.fromEntries(students.map((s) => [UiUtils.fullName(s), s.groupNumber || "Без группы"]));
    const absenceByGroup = {};
    absences.forEach((a) => {
      const groupName = groupByStudent[a.studentName] || "Без группы";
      absenceByGroup[groupName] = (absenceByGroup[groupName] || 0) + Number(a.count || 0);
    });
    const worstAbsenceGroup = Object.entries(absenceByGroup).sort((a, b) => b[1] - a[1])[0];
    const groupAverages = groups.map((g) => {
      const values = grades.filter((gr) => (g.students || []).includes(gr.studentName)).flatMap((gr) => gradeNumbers(gr.value));
      return { group: g.number, avg: values.length ? values.reduce((a, b) => a + b, 0) / values.length : 0 };
    }).filter((x) => x.avg > 0).sort((a, b) => a.avg - b.avg);
    const highestAverageGroup = [...groupAverages].sort((a, b) => b.avg - a.avg)[0];
    return {
      absence: worstAbsenceGroup ? `${worstAbsenceGroup[0]}: ${worstAbsenceGroup[1]} пропусков` : "Нет критичных пропусков",
      lowGroup: groupAverages[0] ? `${groupAverages[0].group}: средний ${groupAverages[0].avg.toFixed(2)}` : "Недостаточно оценок",
      highGroup: highestAverageGroup ? `${highestAverageGroup.group}: средний ${highestAverageGroup.avg.toFixed(2)}` : "Недостаточно оценок"
    };
  }, [absences, grades, groups, students]);
  const saveAdminJournalGrade = async ({ studentName, disciplineName, teacherName, lessonType, date, value }) => {
    const targetDate = date || new Date().toISOString().slice(0, 10);
    const targetLessonType = normalizeLessonType(lessonType);
    const currentGrades = studentName && disciplineName
      ? gradesForJournalCell({ studentName, disciplineName, lessonType: targetLessonType, date: targetDate })
      : [];
    if (!studentName || !disciplineName || !isValidGradeInput(value)) {
      if (String(value ?? "").trim() === "" && studentName && disciplineName) {
        await deleteJournalGrades(currentGrades);
        await loadAll();
        logAdminAction("Оценка", `Удалены оценки: ${studentName} / ${disciplineName}, ${targetDate}`);
        setMessage("Оценки удалены.");
        return;
      }
      setMessage("Выберите студента, предмет и введите оценку от 0 до 10.");
      return;
    }
    if (hasAbsenceOnDate({ studentName, disciplineName, lessonType: targetLessonType, date: targetDate })) {
      setMessage("На эту дату уже стоит пропуск. Оценку поставить нельзя.");
      return;
    }
    const studentId = studentIdByName(studentName);
    const disciplineId = disciplineIdByName(disciplineName);
    if (!studentId || !disciplineId) {
      setMessage("Не удалось определить студента или предмет.");
      return;
    }
    await deleteJournalGrades(currentGrades);
    const savedGrades = [];
    for (const gradeValue of gradeNumbers(value)) {
      const savedGrade = await api.save("grades", null, { value: gradeValue, studentId, disciplineId });
      savedGrades.push(savedGrade);
    }
    setGradeLessonMeta((prev) => {
      const next = { ...prev };
      savedGrades.forEach((savedGrade, index) => {
        next[savedGrade?.id ? `grade:${savedGrade.id}` : `${gradeMetaKey(studentName, disciplineName)}__${targetDate}__${targetLessonType}__${index}`] = {
          lessonType: targetLessonType,
          teacherName: teacherName || disciplines.find((d) => d.name === disciplineName)?.teacherName || "",
          date: targetDate,
          updatedAt: new Date().toLocaleString("ru-RU"),
          author: "Администратор"
        };
      });
      return next;
    });
    await loadAll();
    logAdminAction("Оценка", `${studentName} / ${disciplineName}: ${value}`);
    setMessage("Оценка добавлена.");
  };
  const saveAdminJournalAbsence = ({ studentName, disciplineName, teacherName, lessonType, date, value, reason }) => {
    if (!studentName || !disciplineName) {
      setMessage("Выберите студента и предмет.");
      return;
    }
    const targetDate = date || new Date().toISOString().slice(0, 10);
    const targetLessonType = normalizeLessonType(lessonType);
    if (String(value ?? "").trim() === "") {
      setAbsences((prev) => prev.filter((absence) => !(
        absence.studentName === studentName
        && absence.disciplineName === disciplineName
        && normalizeLessonType(absence.lessonType) === targetLessonType
        && absence.date === targetDate
      )));
      logAdminAction("Пропуск", `Удалены пропуски: ${studentName} / ${disciplineName}, ${targetDate}`);
      setMessage("Пропуски удалены.");
      return;
    }
    const raw = String(value || "").trim().toUpperCase() === "Н" ? ACADEMIC_HOURS_PER_ABSENCE : Number(value);
    if (Number.isNaN(raw) || raw < 0) {
      setMessage("Введите корректное количество часов пропуска.");
      return;
    }
    const isSick = isStudentOnSickLeaveOnDate(studentName, targetDate);
    const excused = isSick || hasExcuseReason(reason);
    const existingSameDay = absences.filter((absence) => (
      absence.studentName === studentName
      && absence.disciplineName === disciplineName
      && normalizeLessonType(absence.lessonType) === targetLessonType
      && absence.date === targetDate
    ));
    const totalHours = raw;
    const record = {
      id: nextLocalId(),
      studentName,
      disciplineName,
      count: excused ? 0 : Math.max(0, Math.round(raw / ACADEMIC_HOURS_PER_ABSENCE)),
      lessonType: targetLessonType,
      teacherName: teacherName || disciplines.find((d) => d.name === disciplineName)?.teacherName || "",
      date: targetDate,
      reason: isSick ? "Болезнь" : String(reason || "").trim(),
      excusedHours: excused ? totalHours : 0,
      author: "Администратор",
      updatedAt: new Date().toLocaleString("ru-RU")
    };
    setAbsences((prev) => excused
      ? [record, ...prev.filter((absence) => !existingSameDay.some((item) => item.id === absence.id))]
      : [record, ...prev]);
    logAdminAction("Пропуск", `${studentName} / ${disciplineName}: ${raw} ч${reason ? `, причина: ${reason}` : ""}`);
    setMessage("Пропуск добавлен.");
  };
  const adminScheduleRows = useMemo(() => fullSchedule.map((lesson, index) => {
    const conflict = fullSchedule.some((other, otherIndex) => otherIndex !== index
      && other.day === lesson.day
      && Number(other.slot) === Number(lesson.slot)
      && (other.teacher === lesson.teacher || other.room === lesson.room || String(other.group) === String(lesson.group)));
    const changed = Boolean(lesson.id);
    return {
      ...lesson,
      calendarId: lesson.id ? `custom-${lesson.id}` : `base-${lessonKey(lesson)}`,
      conflict,
      changed,
      cancelled: lesson.cancelled
    };
  }), [fullSchedule]);
  const filteredAdminScheduleRows = useMemo(() => adminScheduleRows.filter((lesson) => {
    const f = adminScheduleFilters;
    const q = String(f.query || "").trim().toLowerCase();
    const byQuery = q ? `${lesson.group} ${lesson.teacher} ${lesson.discipline} ${lesson.room}`.toLowerCase().includes(q) : true;
    return (!f.group || String(lesson.group) === String(f.group))
      && (!f.teacher || lesson.teacher === f.teacher)
      && (!f.discipline || lesson.discipline === f.discipline)
      && (!f.room || String(lesson.room).toLowerCase().includes(f.room.toLowerCase()))
      && byQuery
      && (f.viewType === "all" || (f.viewType === "lessons" && !lesson.cancelled) || (f.viewType === "conflicts" && lesson.conflict));
  }), [adminScheduleRows, adminScheduleFilters]);
  const selectedAdminLesson = filteredAdminScheduleRows.find((lesson) => lesson.calendarId === selectedAdminLessonId) || null;
  const fillAdminLessonForm = (lesson) => {
    setSelectedAdminLessonId(lesson.calendarId || null);
    setAdminLessonForm({
      editId: lesson.id || null,
      replacesKey: lesson.id ? (lesson.replacesKey || "") : lessonKey(lesson),
      day: lesson.day,
      slot: Number(lesson.slot),
      discipline: lesson.discipline || "",
      group: lesson.group || adminScheduleFilters.group || "",
      room: lesson.room || "500-к.",
      teacher: lesson.teacher || "",
      lessonType: normalizeLessonType(lesson.lessonType)
    });
  };
  const startAdminLessonAt = (day, slotNumber) => {
    setSelectedAdminLessonId(null);
    setAdminLessonForm((prev) => ({
      ...prev,
      editId: null,
      replacesKey: "",
      day,
      slot: Number(slotNumber),
      group: adminScheduleFilters.group || prev.group,
      discipline: "",
      teacher: "",
      room: "500-к.",
      lessonType: "ЛК"
    }));
  };
  const studentPreview = studentPreviewName ? sortedStudents.find((s) => UiUtils.fullName(s) === studentPreviewName) : null;
  const studentPreviewMeta = studentPreviewName ? studentMetaOf(studentPreviewName) : null;
  const studentPeekGroupSize = useMemo(() => {
    if (!studentPreviewName) return 0;
    const s = sortedStudents.find((x) => UiUtils.fullName(x) === studentPreviewName);
    if (!s?.groupNumber) return 0;
    const g = groups.find((x) => String(x.number) === String(s.groupNumber));
    return (g?.students || []).length;
  }, [studentPreviewName, sortedStudents, groups]);
  const rusWeekday = useMemo(() => {
    const d = new Date();
    return ["Воскресенье", "Понедельник", "Вторник", "Среда", "Четверг", "Пятница", "Суббота"][d.getDay()];
  }, []);

  const todayLessonsSorted = useMemo(
    () => mySchedule.filter((l) => l.day === rusWeekday).slice().sort((a, b) => a.slot - b.slot),
    [mySchedule, rusWeekday]
  );

  const scheduleWithStatus = useMemo(() => {
    const now = new Date().getHours() * 60 + new Date().getMinutes();
    const withTimes = todayLessonsSorted.map((l) => {
      const [t0, t1] = l.time.split("-").map((s) => s.trim());
      const parseT = (t) => {
        const [h, m] = t.split(":").map(Number);
        return h * 60 + m;
      };
      const startM = parseT(t0);
      const endM = parseT(t1);
      let status = "upcoming";
      if (now >= endM) status = "completed";
      else if (now >= startM && now < endM) status = "current";
      return { ...l, status, startM, endM };
    });
    const nextIdx = withTimes.findIndex((l) => l.status === "upcoming");
    if (nextIdx >= 0) {
      withTimes[nextIdx] = { ...withTimes[nextIdx], status: "next" };
    }
    return withTimes;
  }, [todayLessonsSorted]);

  const teacherTodayLessons = useMemo(
    () => (session?.role === "teacher"
      ? teacherSchedule.filter((l) => l.day === rusWeekday).slice().sort((a, b) => a.slot - b.slot)
      : []),
    [session?.role, teacherSchedule, rusWeekday]
  );

  const teacherScheduleWithStatus = useMemo(() => {
    if (session?.role !== "teacher" || teacherTodayLessons.length === 0) return [];
    const now = new Date().getHours() * 60 + new Date().getMinutes();
    const withTimes = teacherTodayLessons.map((l) => {
      const parts = l.time.split("-").map((s) => s.trim());
      const t0 = parts[0] || "0:00";
      const t1 = parts[1] || parts[0] || "23:59";
      const parseT = (t) => {
        const [h, m] = t.split(":").map(Number);
        return h * 60 + m;
      };
      const startM = parseT(t0);
      const endM = parseT(t1);
      let status = "upcoming";
      if (now >= endM) status = "completed";
      else if (now >= startM && now < endM) status = "current";
      return { ...l, status, displayTime: `${t0} – ${t1}` };
    });
    const nextIdx = withTimes.findIndex((l) => l.status === "upcoming");
    if (nextIdx >= 0) {
      withTimes[nextIdx] = { ...withTimes[nextIdx], status: "next" };
    }
    return withTimes;
  }, [session?.role, teacherTodayLessons]);

  const lessonDateByDay = useMemo(() => {
    const now = new Date();
    const mondayBasedToday = (now.getDay() + 6) % 7;
    const map = {};
    UiUtils.days.forEach((dayName, idx) => {
      const shift = idx - mondayBasedToday;
      const date = new Date(now);
      date.setDate(now.getDate() + shift);
      map[dayName] = date.toLocaleDateString("ru-RU", { day: "2-digit", month: "2-digit" });
    });
    return map;
  }, []);

  const teacherDashboardGroups = useMemo(() => {
    if (session?.role !== "teacher") return [];
    const byGroup = {};
    teacherSchedule.forEach((l) => {
      const g = String(l.group);
      if (!byGroup[g]) byGroup[g] = new Set();
      byGroup[g].add(l.discipline);
    });
    return Object.entries(byGroup).map(([num, discSet]) => {
      const subj = [...discSet].join(", ");
      const gObj = groups.find((x) => String(x.number) === num);
      const studs = gObj?.students || [];
      const n = studs.length;
      let sum = 0;
      let cnt = 0;
      studs.forEach((sn) => {
        grades
          .filter((gr) => gr.studentName === sn && discSet.has(gr.disciplineName))
          .forEach((gr) => {
            gradeNumbers(gr.value).forEach((value) => {
              sum += value;
              cnt++;
            });
          });
      });
      const avgNum = cnt ? sum / cnt : null;
      const avg = avgNum != null ? avgNum.toFixed(2) : "—";
      return { number: num, subjectsLabel: subj, studentCount: n, avg, avgNum };
    }).sort((a, b) => a.number.localeCompare(b.number, "ru"));
  }, [session?.role, teacherSchedule, groups, grades]);

  const teacherQuickJournalRows = useMemo(() => {
    if (session?.role !== "teacher") return [];
    const seen = new Set();
    const rows = [];
    teacherSchedule.forEach((l) => {
      const lessonType = normalizeLessonType(l.lessonType);
      const key = `${l.group}|${l.discipline}|${lessonType}`;
      if (seen.has(key)) return;
      seen.add(key);
      const gObj = groups.find((x) => String(x.number) === String(l.group));
      const studs = gObj?.students || [];
      let maxDate = 0;
      let maxLabel = "—";
      studs.forEach((sn) => {
        grades
          .filter((gr) => gr.studentName === sn && gr.disciplineName === l.discipline)
          .forEach((gr) => {
            const t = gr.date ? new Date(gr.date).getTime() : 0;
            if (t > maxDate) {
              maxDate = t;
              maxLabel = gr.date ? new Date(gr.date).toLocaleDateString("ru-RU") : "—";
            }
          });
      });
      rows.push({ group: String(l.group), discipline: l.discipline, lessonType, typeMeta: lessonTypeMeta(lessonType), lastLesson: maxLabel });
    });
    return rows.sort((a, b) => a.group.localeCompare(b.group, "ru") || a.discipline.localeCompare(b.discipline, "ru") || a.lessonType.localeCompare(b.lessonType, "ru"));
  }, [session?.role, teacherSchedule, groups, grades]);

  const teacherJournalDisciplineOptions = useMemo(() => {
    if (session?.role !== "teacher") return [];
    if (!teacherJournalContext.groupNumber) return teacherDisciplines.map((d) => d.name);
    const set = new Set(
      teacherSchedule
        .filter((l) => String(l.group) === String(teacherJournalContext.groupNumber))
        .map((l) => l.discipline)
    );
    return [...set].sort((a, b) => a.localeCompare(b, "ru"));
  }, [session?.role, teacherJournalContext.groupNumber, teacherSchedule, teacherDisciplines]);

  const teacherJournalLessonTypeOptions = useMemo(() => {
    return LESSON_TYPES.map((type) => type.code);
  }, []);

  const teacherJournalStudents = useMemo(() => {
    if (session?.role !== "teacher") return [];
    const groupsForTeacher = teacherGroups.length
      ? teacherGroups
      : [...new Set(teacherStudents.map((s) => String(s.groupNumber || "")).filter(Boolean))];
    const targetGroup = teacherJournalContext.groupNumber || groupsForTeacher[0] || "";
    if (!targetGroup) return [];
    const groupObj = groups.find((g) => String(g.number) === String(targetGroup));
    const listedNames = (groupObj?.students || []).map((name) => String(name).trim()).filter(Boolean);
    if (listedNames.length > 0) {
      return listedNames
        .map((name) => students.find((s) => UiUtils.fullName(s) === name))
        .filter(Boolean)
        .sort((a, b) => UiUtils.fullName(a).localeCompare(UiUtils.fullName(b), "ru"));
    }
    return students
      .filter((s) => String(s.groupNumber) === String(targetGroup))
      .sort((a, b) => UiUtils.fullName(a).localeCompare(UiUtils.fullName(b), "ru"));
  }, [session?.role, teacherJournalContext.groupNumber, teacherGroups, teacherStudents, groups, students]);

  const totalAbsenceCount = useMemo(
    () => myAbsences.reduce((s, a) => s + (hasExcuseReason(a.reason) ? 0 : Number(a.count || 0)), 0),
    [myAbsences]
  );
  const totalAbsenceHours = totalAbsenceCount * ACADEMIC_HOURS_PER_ABSENCE;

  const gradeBuckets = useMemo(() => {
    const b = { excellent: 0, good: 0, satisfactory: 0, poor: 0 };
    myGrades.forEach((g) => {
      gradeNumbers(g.value).forEach((v) => {
        if (v >= 9) b.excellent++;
        else if (v >= 7) b.good++;
        else if (v >= 5) b.satisfactory++;
        else b.poor++;
      });
    });
    return b;
  }, [myGrades]);

  const gradeTotalCount =
    gradeBuckets.excellent + gradeBuckets.good + gradeBuckets.satisfactory + gradeBuckets.poor;

  const donutGradient = useMemo(() => {
    if (gradeTotalCount === 0) {
      return "conic-gradient(#e8e8f0 0deg 360deg)";
    }
    let a = 0;
    const segs = [
      [gradeBuckets.excellent, "#7e57c2"],
      [gradeBuckets.good, "#ec407a"],
      [gradeBuckets.satisfactory, "#ffca28"],
      [gradeBuckets.poor, "#64b5f6"]
    ];
    const parts = [];
    segs.forEach(([n, color]) => {
      if (!n) return;
      const deg = (n / gradeTotalCount) * 360;
      parts.push(`${color} ${a}deg ${a + deg}deg`);
      a += deg;
    });
    return `conic-gradient(${parts.join(", ")})`;
  }, [gradeBuckets, gradeTotalCount]);

  const todayLessonsCount = todayLessonsSorted.length;
  const scheduleWithStatusFiltered = useMemo(() => {
    const q = studentHomeSearch.trim().toLowerCase();
    if (!q) return scheduleWithStatus;
    return scheduleWithStatus.filter((l) =>
      `${l.discipline} ${l.teacher} ${l.room}`.toLowerCase().includes(q)
    );
  }, [scheduleWithStatus, studentHomeSearch]);

  const myFeedHomeFiltered = useMemo(() => {
    const q = studentHomeSearch.trim().toLowerCase();
    if (!q) return myFeed;
    return myFeed.filter((p) => p.text.toLowerCase().includes(q));
  }, [myFeed, studentHomeSearch]);

  const myGroupMembersHomeFiltered = useMemo(() => {
    const q = studentHomeSearch.trim().toLowerCase();
    if (!q) return myGroupMembers;
    return myGroupMembers.filter((n) => n.toLowerCase().includes(q));
  }, [myGroupMembers, studentHomeSearch]);

  const absenceDashboard = useMemo(() => {
    const H = ACADEMIC_HOURS_PER_ABSENCE;
    const unexcusedUnits = myAbsences.reduce((s, a) => s + (hasExcuseReason(a.reason) ? 0 : Number(a.count || 0)), 0);
    const excusedUnits = myAbsences.reduce((s, a) => (
      s + (hasExcuseReason(a.reason) ? Number(a.count || 0) : 0) + Math.round(Number(a.excusedHours || 0) / H)
    ), 0);
    const rawTotal = unexcusedUnits;
    const allTotal = unexcusedUnits;
    const totalHours = allTotal * H;
    const excusedHours = excusedUnits * H;
    const unexcused = unexcusedUnits * H;
    const trackedHours = unexcused + excusedHours;
    const excusedPct = trackedHours ? Math.round((excusedHours / trackedHours) * 100) : 0;
    const unexcusedPct = trackedHours ? Math.round((unexcused / trackedHours) * 100) : 0;
    const attendanceRate = allTotal === 0 ? 100 : Math.max(72, Math.min(99, 100 - Math.round(allTotal * 1.8)));
    const subjectMap = new Map();
    myAbsences.forEach((a) => {
      const n = hasExcuseReason(a.reason) ? 0 : Number(a.count || 0);
      const hours = n * H;
      if (!hours) return;
      const name = String(a.disciplineName || "Не указан предмет").trim();
      const prev = subjectMap.get(name) || 0;
      subjectMap.set(name, prev + hours);
    });
    const subjShares = Array.from(subjectMap.entries())
      .map(([name, hours]) => ({ name, hours }))
      .sort((a, b) => b.hours - a.hours)
      .map((entry, idx) => ({
        ...entry,
        n: Math.round(entry.hours / H),
        pct: totalHours ? Math.round((entry.hours / totalHours) * 100) : 0,
        colorIndex: idx % 7
      }));
    const donutTotal = allTotal || 1;
    let angle = 0;
    const segs = [];
    const colors = ["#a58bd6", "#f28ab7", "#8fcdf2", "#f4d889", "#9ed7b5", "#c9a7df", "#f0a3a3"];
    subjShares.forEach((s) => {
      const deg = (s.n / donutTotal) * 360;
      segs.push(`${colors[s.colorIndex % colors.length]} ${angle}deg ${angle + deg}deg`);
      angle += deg;
    });
    const absenceDonutBg = segs.length ? `conic-gradient(${segs.join(", ")})` : "conic-gradient(#e8e8f0 0deg 360deg)";
    const weekLabels = ["Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс"];
    const weekTotals = weekLabels.map(() => 0);
    const today = new Date();
    const weekStart = new Date(today);
    weekStart.setHours(0, 0, 0, 0);
    weekStart.setDate(today.getDate() - ((today.getDay() + 6) % 7));
    const weekEnd = new Date(weekStart);
    weekEnd.setDate(weekStart.getDate() + 7);
    myAbsences.forEach((a) => {
      const isoDate = a.date || (() => {
        const match = String(a.updatedAt || "").match(/(\d{2})\.(\d{2})\.(\d{4})/);
        return match ? `${match[3]}-${match[2]}-${match[1]}` : "";
      })();
      const date = isoDate ? new Date(`${isoDate}T00:00:00`) : null;
      if (!date || Number.isNaN(date.getTime()) || date < weekStart || date >= weekEnd) return;
      const dayIdx = (date.getDay() + 6) % 7;
      weekTotals[dayIdx] += hasExcuseReason(a.reason) ? 0 : Number(a.count || 0) * ACADEMIC_HOURS_PER_ABSENCE;
    });
    return {
      rawTotal,
      allTotal,
      totalHours,
      excused: excusedHours,
      unexcused,
      excusedPct,
      unexcusedPct,
      attendanceRate,
      absenceDonutBg,
      subjShares,
      weekLabels,
      weekTotals
    };
  }, [myAbsences]);

  const absenceHistoryRows = useMemo(() => {
    const rows = [];
    myAbsences.forEach((a) => {
      const teacher = mySchedule.find((l) => l.discipline === a.disciplineName)?.teacher || "—";
      const n = Number(a.count || 0);
      const unexcusedHours = n * ACADEMIC_HOURS_PER_ABSENCE;
      const excusedHours = Number(a.excusedHours || 0);
      if (unexcusedHours > 0 && !hasExcuseReason(a.reason)) {
        rows.push({
          id: `${a.id}-u`,
          subject: a.disciplineName,
          hours: unexcusedHours,
          type: "Неуважительная",
          reason: a.reason || "Отсутствовал(а)",
          teacher,
          status: "Подтверждено",
          updatedAt: a.date || a.updatedAt || "—"
        });
      }
      if (unexcusedHours > 0 && hasExcuseReason(a.reason)) {
        rows.push({
          id: `${a.id}-r`,
          subject: a.disciplineName,
          hours: unexcusedHours,
          type: "Уважительная",
          reason: a.reason,
          teacher,
          status: "Подтверждено",
          updatedAt: a.date || a.updatedAt || "—"
        });
      }
      if (excusedHours > 0) {
        rows.push({
          id: `${a.id}-e`,
          subject: a.disciplineName,
          hours: excusedHours,
          type: "Уважительная",
          reason: a.reason || "Больничный",
          teacher,
          status: "Подтверждено",
          updatedAt: a.date || a.updatedAt || "—"
        });
      }
    });
    return rows.sort((a, b) => String(a.subject).localeCompare(String(b.subject), "ru") || String(a.updatedAt).localeCompare(String(b.updatedAt), "ru"));
  }, [myAbsences, mySchedule]);

  useEffect(() => {
    if (session?.role !== "student") return;
    if (studentTab !== "group") setGroupMembersPage(1);
  }, [session?.role, studentTab]);

  useEffect(() => {
    if (session?.role !== "student" || !currentAccount) return;
    setSettingsForm({
      phone: formatBelarusPhone(currentAccount.phone || "+375"),
      email: currentAccount.email || "",
      birthDate: studentProfiles[session.studentName]?.birthDate || ""
    });
  }, [session, currentAccount, studentProfiles]);

  useEffect(() => {
    if (session?.role !== "teacher" || teacherTab !== "journal") return;
    if (!teacherJournalContext.groupNumber && teacherGroups.length) {
      setTeacherJournalContext((prev) => ({ ...prev, groupNumber: teacherGroups[0] }));
    }
  }, [session?.role, teacherTab, teacherJournalContext.groupNumber, teacherGroups]);

  useEffect(() => {
    if (session?.role !== "teacher" || teacherTab !== "journal") return;
    const d = teacherJournalContext.disciplineName;
    if (!d) return;
    setAbsenceForm((p) => ({ ...p, disciplineName: d }));
  }, [session?.role, teacherTab, teacherJournalContext.disciplineName]);

  const adminMenu = [
    { id: "dashboard", label: "Главная", icon: "home" },
    { id: "students", label: "Студенты", icon: "students" },
    { id: "teachers", label: "Преподаватели", icon: "teacher" },
    { id: "disciplines", label: "Дисциплины", icon: "discipline" },
    { id: "faculties", label: "Факультеты и специальности", icon: "faculty" },
    { id: "groups", label: "Группы", icon: "group" },
    { id: "grades", label: "Журнал", icon: "journal" },
    { id: "schedule", label: "Расписание", icon: "calendar" },
    { id: "access", label: "Доступ преподавателей", icon: "access" }
  ];
  const activeAdminFaculty = facultiesCatalog.includes(selectedAdminFaculty) ? selectedAdminFaculty : (facultiesCatalog[0] || "");

  const renderPager = (totalItems, page, setPage) => {
    const totalPages = Math.max(1, Math.ceil(totalItems / PAGE_SIZE));
    if (totalPages <= 1) return null;
    return (
      <div className="row">
        <button className="btn-ghost" onClick={() => setPage((p) => Math.max(1, p - 1))}>Назад</button>
        <span className="sub">{`Стр. ${page} / ${totalPages}`}</span>
        <button className="btn-ghost" onClick={() => setPage((p) => Math.min(totalPages, p + 1))}>Вперед</button>
      </div>
    );
  };

  if (!session) {
    return (
      <div className="login-wrap">
        <div className="sakura-layer" aria-hidden="true">
          {Array.from({ length: 12 }).map((_, idx) => <span key={idx} className="petal" style={{ "--d": `${idx * 1.05}s`, "--x": `${(idx * 9) % 100}%`, "--s": `${9 + (idx % 5)}s` }} />)}
        </div>
        <div className="login-shell">
          <div className="login-visual">
            <div className="login-visual-frame">
              <img className="login-visual-img login-visual-cutout" src="/login-illustration.png" alt="Иллюстрация обучения" />
            </div>
          </div>
          <div className="login-panel">
            <h2 className="login-title">Добро пожаловать</h2>
            <p className="sub">Портал для студентов и преподавателей</p>
            {message && <p className="msg">{message}</p>}
            <div className="auth-switch">
              <button className={authMode === "login" ? "btn-main" : "btn-ghost"} onClick={() => setAuthMode("login")}>Вход</button>
              <button className={authMode === "register" ? "btn-main" : "btn-ghost"} onClick={() => setAuthMode("register")}>Регистрация</button>
            </div>
            {authMode === "login" && (
              <form className="auth-panel" onSubmit={(e) => { e.preventDefault(); doUserLogin(); }}>
                <div className="row input-with-icon"><span className="field-icon"><ActionIcon name="support" /></span><input name="username" autoComplete="username" placeholder="Логин или электронная почта" value={userLogin.loginOrEmail} onChange={(e) => setUserLogin((p) => ({ ...p, loginOrEmail: e.target.value }))} /></div>
                <div className="row input-with-icon"><span className="field-icon"><ActionIcon name="access" /></span><input name="password" autoComplete="current-password" type="password" placeholder="Пароль" value={userLogin.password} onChange={(e) => setUserLogin((p) => ({ ...p, password: e.target.value }))} /></div>
                <button type="submit" className="btn-main wide">Войти</button>
                <button type="button" className="auth-link-btn" onClick={() => { setAuthMode("reset"); setMessage(""); }}>Забыли пароль?</button>
              </form>
            )}
            {authMode === "reset" && (
              <form className="auth-panel" onSubmit={(e) => { e.preventDefault(); resetPasswordByEmail(); }}>
                <div className="row input-with-icon"><span className="field-icon"><ActionIcon name="support" /></span><input autoComplete="email" placeholder="Электронная почта" value={resetPasswordForm.email} onChange={(e) => setResetPasswordForm((p) => ({ ...p, email: e.target.value }))} /></div>
                <div className="row input-with-icon"><span className="field-icon"><ActionIcon name="access" /></span><input autoComplete="new-password" type="password" placeholder="Новый пароль" value={resetPasswordForm.newPassword} onChange={(e) => setResetPasswordForm((p) => ({ ...p, newPassword: e.target.value }))} /></div>
                <div className="row input-with-icon"><span className="field-icon"><ActionIcon name="access" /></span><input autoComplete="new-password" type="password" placeholder="Повторите пароль" value={resetPasswordForm.confirmPassword} onChange={(e) => setResetPasswordForm((p) => ({ ...p, confirmPassword: e.target.value }))} /></div>
                <button type="submit" className="btn-main wide">Восстановить пароль</button>
                <button type="button" className="auth-link-btn" onClick={() => { setAuthMode("login"); setMessage(""); }}>Вернуться ко входу</button>
              </form>
            )}
            {authMode === "register" && (
              <form className="auth-panel" onSubmit={(e) => { e.preventDefault(); doUserRegister(); }}>
                <div className="row input-with-icon"><span className="field-icon"><ActionIcon name="students" /></span><input autoComplete="family-name" placeholder="Фамилия" value={registerForm.lastName} onChange={(e) => setRegisterForm((p) => ({ ...p, lastName: e.target.value }))} /></div>
                <div className="row input-with-icon"><span className="field-icon"><ActionIcon name="students" /></span><input autoComplete="given-name" placeholder="Имя" value={registerForm.firstName} onChange={(e) => setRegisterForm((p) => ({ ...p, firstName: e.target.value }))} /></div>
                <div className="row input-with-icon"><span className="field-icon"><ActionIcon name="students" /></span><input autoComplete="additional-name" placeholder="Отчество" value={registerForm.middleName} onChange={(e) => setRegisterForm((p) => ({ ...p, middleName: e.target.value }))} /></div>
                <div className="row input-with-icon"><span className="field-icon"><ActionIcon name="calendar" /></span><input autoComplete="bday" type="date" value={registerForm.birthDate} onChange={(e) => setRegisterForm((p) => ({ ...p, birthDate: e.target.value }))} /></div>
                <div className="row input-with-icon"><span className="field-icon"><ActionIcon name="support" /></span><input placeholder="+375-25-501-23-91" value={registerForm.phone} onChange={(e) => setRegisterForm((p) => ({ ...p, phone: formatBelarusPhone(e.target.value) }))} /></div>
                <div className="row input-with-icon"><span className="field-icon"><ActionIcon name="support" /></span><input autoComplete="email" placeholder="Электронная почта" value={registerForm.email} onChange={(e) => setRegisterForm((p) => ({ ...p, email: e.target.value }))} /></div>
                <div className="row input-with-icon"><span className="field-icon"><ActionIcon name="access" /></span><input autoComplete="new-password" type="password" placeholder="Пароль" value={registerForm.password} onChange={(e) => setRegisterForm((p) => ({ ...p, password: e.target.value }))} /></div>
                <button type="submit" className="btn-soft wide">Создать аккаунт</button>
              </form>
            )}
          </div>
        </div>
      </div>
    );
  }

  const renderStudentPeekModal = () => {
    if (!studentPreviewName || !studentPreview) return null;
    const previewAccount = userAccounts.find((a) => a.fullName === studentPreviewName);
    return (
      <div className="note-modal-backdrop edu-mini-profile-backdrop" onClick={() => setStudentPreviewName(null)}>
        <div className="edu-mini-profile" onClick={(e) => e.stopPropagation()} role="dialog" aria-label="Профиль студента">
          <section className="edu-mini-profile-compact-hero">
            <div className="edu-mini-profile-avatar-wrap">
              <img src={avatars[studentPreview.id] || ""} alt="" />
            </div>
            <div className="edu-mini-profile-main">
              <h2 className="edu-mini-profile-name">{studentPreviewName}</h2>
              <p className="edu-mini-profile-meta">
                <span><ActionIcon name="group" /> Группа {studentPreview.groupNumber || "—"}</span>
                <span><ActionIcon name="course" /> {formatCourseDisplay(studentPreviewMeta?.course)}</span>
              </p>
              <p className="edu-mini-profile-meta">
                <span><ActionIcon name="faculty" /> {studentPreviewMeta?.faculty || "Факультет не указан"}{studentPreviewMeta?.specialty ? `, ${studentPreviewMeta.specialty}` : ""}</span>
              </p>
              <p className="edu-mini-profile-birth">День рождения: {formatBirthRu(studentPreviewMeta?.birthDate)}</p>
            </div>
          </section>
          <div className="edu-mini-profile-contact-grid">
            <article>
              <span>Телефон</span>
              <strong>{previewAccount?.phone ? formatBelarusPhone(previewAccount.phone) : "Не указан"}</strong>
            </article>
            <article>
              <span>Email</span>
              <strong>{previewAccount?.email || "Не указан"}</strong>
            </article>
            <article>
              <span>Средний балл</span>
              <strong className="edu-mini-profile-gpa">{avgMap[studentPreviewName] || "—"}</strong>
            </article>
            <article>
              <span>В группе</span>
              <strong>{studentPeekGroupSize ? `${studentPeekGroupSize} студентов` : "—"}</strong>
            </article>
          </div>
        </div>
      </div>
    );
  };

  if (session.role === "student") {
    const effectiveStudentTab = studentTab === "quotes" ? "profile" : studentTab;
    const studentNav = [
      ["profile", "home", "Главная"],
      ["schedule", "calendar", "Расписание"],
      ["grades", "grade", "Оценки"],
      ["absences", "absence", "Пропуски"],
      ["notes", "notes", "Заметки"],
      ["group", "students", "Группа"]
    ];
    return (
      <div className={`app-layout student-app-layout student-theme-${studentTheme}`}>
        <aside className="sidebar student-sidebar edu-sidebar student-sidebar--collapse">
          <div className="brand edu-brand"><span className="edu-brand-full">EduFlow</span><span className="edu-brand-short" aria-hidden>E</span></div>
          <nav className="menu student-side-menu">
            {studentNav.map(([id, icon, label]) => (
              <button
                key={id}
                type="button"
                className={effectiveStudentTab === id ? "active" : ""}
                onClick={() => setStudentTab(id)}
              >
                <span className="menu-icon" aria-hidden><SidebarIcon name={icon} /></span>
                <span className="menu-label">{label}</span>
              </button>
            ))}
          </nav>
          <div className="student-sidebar-foot">
            <button type="button" className={effectiveStudentTab === "settings" ? "active student-side-aux" : "student-side-aux"} onClick={() => {
              setPasswordForm({ oldPassword: "", newPassword: "", confirmPassword: "" });
              setSettingsMessage("");
              setStudentTab("settings");
            }}>
              <span className="menu-icon" aria-hidden><SidebarIcon name="settings" /></span>
              <span className="menu-label">Настройки</span>
            </button>
            <button type="button" className="student-side-aux" onClick={logout}>
              <span className="menu-icon" aria-hidden><SidebarIcon name="logout" /></span>
              <span className="menu-label">Выход</span>
            </button>
          </div>
        </aside>
        <div className={`student-shell edu-student-shell edu-student-shell--template sakura-bg student-theme-${studentTheme}`}>
          <div className="sakura-layer" aria-hidden="true">
            {Array.from({ length: 10 }).map((_, idx) => <span key={idx} className="petal" style={{ "--d": `${idx * 1.2}s`, "--x": `${(idx * 7) % 100}%`, "--s": `${8 + (idx % 6)}s` }} />)}
          </div>
          <main className="main student-dashboard-main">
            <header className="edu-topbar edu-topbar--student">
              <div className="edu-topbar-title-slot">
                <span className="edu-topbar-crumb">EduFlow</span>
              </div>
              <div className="edu-topbar-right">
                <input
                  ref={studentAvatarFileRef}
                  className="avatar-file-input"
                  type="file"
                  accept="image/*"
                  onChange={(e) => {
                    const f = e.target.files?.[0];
                    if (f && myStudent) uploadAvatar(myStudent, f);
                    e.target.value = "";
                  }}
                />
                <button type="button" className="edu-top-user" onClick={() => setStudentTab("profile")} title="Перейти в профиль">
                  <span className="edu-top-avatar-wrap">
                    <img className="edu-top-avatar" src={myStudent ? (avatars[myStudent.id] || "") : ""} alt="" />
                  </span>
                  <span className="edu-top-user-text">
                    <span className="edu-top-user-name">{shortStudentName(session.studentName)}</span>
                    <span className="edu-top-user-role">Студент · {profileCourse}</span>
                  </span>
                </button>
              </div>
            </header>
            <div className="student-dashboard-body">
              {effectiveStudentTab === "profile" && (
                <StudentProfile
                  avatars={avatars}
                  donutGradient={donutGradient}
                  gradeTotalCount={gradeTotalCount}
                  myAvg={myAvg}
                  myFeedHomeFiltered={myFeedHomeFiltered}
                  myGroupMembersHomeFiltered={myGroupMembersHomeFiltered}
                  myStudent={myStudent}
                  onEditAvatar={() => studentAvatarFileRef.current?.click()}
                  onOpenNewNote={() => setShowNewNoteModal(true)}
                  onOpenStudentPreview={setStudentPreviewName}
                  onStartEditPost={startEditPost}
                  profileCourse={profileCourse}
                  profileFaculty={profileFaculty}
                  profileSpecialty={profileSpecialty}
                  rusWeekday={rusWeekday}
                  scheduleWithStatusFiltered={scheduleWithStatusFiltered}
                  session={{
                    ...session,
                    birthDateText: formatBirthRu(studentProfiles[session.studentName]?.birthDate),
                    totalAbsenceHours
                  }}
                  setStudentTab={setStudentTab}
                  shortStudentName={shortStudentName}
                  studentTheme={studentTheme}
                  students={students}
                  todayLessonsCount={todayLessonsCount}
                  onChangeStudentTheme={setStudentTheme}
                  UiUtils={UiUtils}
                />
              )}

              <div className="student-tab-sections">
              {effectiveStudentTab === "notes" && (
                <div className="student-tab-page">
                  <header className="student-tab-page-head student-tab-page-head--row">
                    <div>
                      <h2 className="student-tab-page-title">Заметки</h2>
                    </div>
                  </header>
                  <article className="edu-widget student-tab-panel student-panel-unified">
                    <div className="notes-board notes-board--tab">
                      {myFeed.length === 0 && <p className="edu-widget-empty">Пока нет заметок.</p>}
                      {myFeed.map((post, idx) => (
                        <article
                          className={`note-sticky note-variant-${idx % 4}`}
                          key={post.id}
                          onClick={() => startEditPost(post)}
                          onKeyDown={(e) => { if (e.key === "Enter") startEditPost(post); }}
                          role="button"
                          tabIndex={0}
                        >
                          <p className="note-text">{post.text}</p>
                          <div className="post-menu-wrap">
                            <button
                              type="button"
                              className="post-menu-button"
                              onClick={(e) => {
                                e.stopPropagation();
                                setOpenPostMenuId((prev) => prev === post.id ? null : post.id);
                              }}
                            >
                              ...
                            </button>
                            {openPostMenuId === post.id && (
                              <div className="post-menu-dropdown">
                                <button type="button" className="btn-ghost" onClick={(e) => { e.stopPropagation(); startEditPost(post); }} title="Редактировать" aria-label="Редактировать запись">Редактировать</button>
                                <button type="button" className="btn-ghost" onClick={(e) => { e.stopPropagation(); removePost(post.id); }} title="Удалить" aria-label="Удалить запись">Удалить</button>
                              </div>
                            )}
                          </div>
                          <div className="note-date note-date-bottom"><span>{post.createdAt}</span></div>
                        </article>
                      ))}
                    </div>
                  </article>
                  <button type="button" className="edu-floating-add-note" onClick={() => setShowNewNoteModal(true)} aria-label="Добавить заметку">
                    <span className="edu-floating-add-note-plus">+</span>
                    <span className="edu-floating-add-note-text">Добавить заметку</span>
                  </button>
                </div>
              )}
              {effectiveStudentTab === "schedule" && (
                <div className="student-tab-page">
                  <header className="student-tab-page-head">
                    <h2 className="student-tab-page-title">Расписание занятий</h2>
                  </header>
                  <article className="edu-widget student-tab-panel">
                    <div className="row student-schedule-search">
                      <input className="student-schedule-search-input" placeholder="Поиск расписания" value={scheduleSearch} onChange={(e) => setScheduleSearch(e.target.value)} />
                    </div>
                    <div className="admin-calendar-grid admin-calendar-grid--six role-schedule-grid">
                      <div className="admin-calendar-corner" />
                      {scheduleDays.map((day) => <div key={day} className="admin-calendar-head">{dayShortName(day)}<span>{day} · {lessonDateByDay[day] || ""}</span></div>)}
                      {visibleStudentLessonSlots.map((slot) => (
                        <Fragment key={slot.number}>
                          <div className="admin-calendar-time">{slot.start}<span>{slot.end}</span></div>
                          {scheduleDays.map((day) => {
                            const lessons = searchableSchedule.find((block) => block.day === day)?.lessons.filter((lesson) => Number(lesson.slot) === Number(slot.number)) || [];
                            return (
                              <div key={`${day}-${slot.number}`} className="admin-calendar-cell">
                                {lessons.map((s, idx) => {
                                  const teacherPreview = teacherProfileOf(s.teacher);
                                  return (
                                    <div role="button" tabIndex={0} className={`admin-lesson-card color-${idx % 5}`} key={`${day}-${slot.number}-${s.discipline}-${s.lessonType}`} onClick={() => setStudentTab("grades")} onKeyDown={(e) => e.key === "Enter" && setStudentTab("grades")}>
                                      <strong className="schedule-discipline-name" title={disciplineFullName(s.discipline)}>{s.discipline}</strong>
                                      <span>{s.typeMeta?.label || s.lessonType} · ауд. {s.room}</span>
                                      <span className="schedule-teacher-wrap">
                                        <button type="button" className="schedule-teacher-link" onClick={(e) => { e.stopPropagation(); setTeacherPreviewName(s.teacher); }} title="Открыть профиль преподавателя">
                                          {s.teacher}
                                        </button>
                                        <span className="schedule-teacher-hover" aria-hidden="true">
                                          <img src={teacherPreview.avatar || ""} alt="" />
                                          <strong>{s.teacher}</strong>
                                          <small>{teacherPreview.department || "Кафедра не указана"}</small>
                                        </span>
                                      </span>
                                    </div>
                                  );
                                })}
                              </div>
                            );
                          })}
                        </Fragment>
                      ))}
                    </div>
                  </article>
                </div>
              )}
              {effectiveStudentTab === "grades" && (
                <div className="student-tab-page">
                  <header className="student-tab-page-head">
                    <h2 className="student-tab-page-title">Успеваемость</h2>
                  </header>
                  <div className="student-grade-filters edu-widget">
                    <select aria-label="Семестр" value={studentGradeFilters.semester} onChange={(e) => setStudentGradeFilters((prev) => ({ ...prev, semester: e.target.value }))}>
                      <option value="spring">Весенний семестр</option>
                      <option value="autumn">Осенний семестр</option>
                    </select>
                    <select aria-label="Предмет" value={studentGradeFilters.subject} onChange={(e) => setStudentGradeFilters((prev) => ({ ...prev, subject: e.target.value }))}>
                      <option value="">Все предметы</option>
                      {myJournal.cards.map((card) => <option key={card.subject} value={card.subject}>{card.subject}</option>)}
                    </select>
                    <select aria-label="Тип занятия" value={studentGradeFilters.lessonType} onChange={(e) => setStudentGradeFilters((prev) => ({ ...prev, lessonType: e.target.value }))}>
                      <option value="">Все типы</option>
                      {LESSON_TYPES.map((type) => <option key={type.code} value={type.code}>{type.title}</option>)}
                    </select>
                  </div>
                  <section className="student-grade-summary">
                    <article className="grade-summary-card"><span>Средний балл</span><strong>{myJournal.summary.avg}</strong></article>
                    <article className="grade-summary-card"><span>Лучший предмет</span><strong>{myJournal.summary.best}</strong></article>
                    <article className="grade-summary-card"><span>Худший предмет</span><strong>{myJournal.summary.worst}</strong></article>
                    <article className="grade-summary-card"><span>Пропуски</span><strong>{myJournal.summary.totalAbsences} ч</strong></article>
                  </section>
                  <div className="student-subject-cards">
                    {filteredJournalCards.map((card) => (
                      <article className="edu-widget student-subject-card" key={card.subject}>
                        <div className="student-subject-head">
                          <div>
                            <h3>{card.subject}</h3>
                            <p>Средний балл: <strong>{card.avg}</strong> | Пропуски: <strong>{card.absences} ч</strong></p>
                          </div>
                        </div>
                        <div className="lesson-type-groups">
                          {card.groups.map((group) => (
                            <section className={`lesson-type-group lesson-type-group--${group.typeMeta.key}`} key={`${card.subject}-${group.lessonType}`}>
                              <div className="lesson-type-group-head">
                                <span className={`lesson-type-tag lesson-type-tag--${group.typeMeta.key}`}>{group.typeMeta.icon} {group.typeMeta.title}</span>
                                <span className="lesson-type-teacher">({group.teacherName})</span>
                              </div>
                              <div className="lesson-grade-row">
                                {group.grades.length ? group.grades.map((grade, gradeIdx) => (
                                  <span
                                    className="grade-pill lesson-grade-pill"
                                    key={`${grade.id || gradeIdx}-${group.lessonType}`}
                                    title={`Оценка: ${grade.value}\nТип: ${group.typeMeta.label}\nПреподаватель: ${grade.teacherName}\nДата: ${grade.dateLabel}`}
                                  >
                                    {grade.value}
                                  </span>
                                )) : <span className="grade-empty">—</span>}
                              </div>
                              <p className="lesson-type-average">Средний: {group.avg}</p>
                            </section>
                          ))}
                        </div>
                      </article>
                    ))}
                    {filteredJournalCards.length === 0 ? <p className="edu-widget-empty">Оценок пока нет.</p> : null}
                  </div>
                </div>
              )}
              {effectiveStudentTab === "absences" && (
                <div className="absences-dashboard">
                  <h2 className="abs-dash-page-title">Пропуски</h2>
                  <div className="abs-dash-stats">
                    <article className="abs-dash-stat abs-dash-stat--total">
                      <span className="abs-dash-stat-label">Всего часов пропусков</span>
                      <strong className="abs-dash-stat-val">{absenceDashboard.totalHours} ч</strong>
                      <span className="abs-dash-stat-sub">за всё время</span>
                    </article>
                    <article className="abs-dash-stat abs-dash-stat--ok">
                      <span className="abs-dash-stat-label">Уважительные</span>
                      <strong className="abs-dash-stat-val">{absenceDashboard.excused} ч <span className="abs-dash-pct">({absenceDashboard.excusedPct}%)</span></strong>
                    </article>
                    <article className="abs-dash-stat abs-dash-stat--bad">
                      <span className="abs-dash-stat-label">Неуважительные</span>
                      <strong className="abs-dash-stat-val">{absenceDashboard.unexcused} ч <span className="abs-dash-pct">({absenceDashboard.unexcusedPct}%)</span></strong>
                    </article>
                    <article className="abs-dash-stat abs-dash-stat--rate">
                      <span className="abs-dash-stat-label">Посещаемость</span>
                      <strong className="abs-dash-stat-val">{absenceDashboard.attendanceRate}%</strong>
                      <span className="abs-dash-stat-good">хороший показатель</span>
                    </article>
                  </div>
                  <div className="abs-dash-mid">
                    <article className="edu-widget abs-dash-chart-card">
                      <h3 className="edu-widget-title">Динамика пропусков</h3>
                      <p className="abs-dash-chart-legend">Количество пропусков за текущую неделю</p>
                      <div className="abs-week-chart">
                        <div className="abs-week-y-axis" aria-hidden>
                          {[Math.max(1, ...absenceDashboard.weekTotals), Math.ceil(Math.max(1, ...absenceDashboard.weekTotals) / 2), 0].map((tick, idx) => <span key={`${tick}-${idx}`}>{tick} ч</span>)}
                        </div>
                        <div className="abs-bars-chart">
                        {absenceDashboard.weekLabels.map((day, i) => {
                          const val = absenceDashboard.weekTotals[i] || 0;
                          const maxVal = Math.max(1, ...absenceDashboard.weekTotals);
                          const h = val > 0 ? Math.max(8, Math.round((val / maxVal) * 120)) : 0;
                          return (
                            <div key={day} className="abs-bar-col">
                              <span className="abs-bar-val">{val} ч</span>
                              <div className="abs-bar-track">
                                <span className="abs-bar-fill" style={{ height: `${h}px` }} />
                              </div>
                              <span className="abs-bar-month">{day}</span>
                            </div>
                          );
                        })}
                        </div>
                      </div>
                    </article>
                    <article className="edu-widget abs-dash-donut-card">
                      <h3 className="edu-widget-title">По предметам</h3>
                      <div className="abs-donut-row">
                        <div className="grade-donut-wrap abs-donut-wrap">
                          <div className="grade-donut" style={{ background: absenceDashboard.absenceDonutBg }} />
                          <div className="grade-donut-hole">
                            <span className="grade-donut-avg">{absenceDashboard.totalHours} ч</span>
                            <span className="grade-donut-sub">акад. ч.</span>
                          </div>
                        </div>
                        <ul className="abs-donut-legend">
                          {absenceDashboard.subjShares.map((s) => (
                            <li key={s.name}>
                              <span className={`abs-legend-dot c${s.colorIndex % 7}`} />
                              {s.name} <span className="abs-legend-pct">{s.hours} ч. · {s.pct}%</span>
                            </li>
                          ))}
                          {absenceDashboard.subjShares.length === 0 ? <li className="sub">Нет данных</li> : null}
                        </ul>
                      </div>
                    </article>
                  </div>
                  <div className="abs-dash-bottom">
                    <article className="edu-widget abs-dash-table-card">
                      <h3 className="edu-widget-title">История пропусков</h3>
                      <div className="abs-table-wrap">
                        <table className="abs-history-table">
                          <thead>
                            <tr>
                              <th>Обновлено</th>
                              <th>Предмет</th>
                              <th>Часы</th>
                              <th>Тип</th>
                              <th>Причина</th>
                              <th>Преподаватель</th>
                              <th>Статус</th>
                            </tr>
                          </thead>
                          <tbody>
                            {paginate(absenceHistoryRows, studentAbsencesPage).map((row) => (
                              <tr key={row.id} className={row.type === "Уважительная" ? "abs-history-row--excused" : ""}>
                                <td>{row.updatedAt}</td>
                                <td>{row.subject}</td>
                                <td>{row.hours} ч</td>
                                <td><span className={row.type === "Уважительная" ? "abs-type exc" : "abs-type unexc"}>{row.type}</span></td>
                                <td>{row.reason}</td>
                                <td>{row.teacher}</td>
                                <td>{row.status}</td>
                              </tr>
                            ))}
                            {absenceHistoryRows.length === 0 ? (
                              <tr><td colSpan={7} className="sub">Пропусков пока нет.</td></tr>
                            ) : null}
                          </tbody>
                        </table>
                      </div>
                      {renderPager(absenceHistoryRows.length, studentAbsencesPage, setStudentAbsencesPage)}
                    </article>
                    <aside className="abs-dash-aside">
                      <article className="edu-widget abs-info-card abs-info-card--explain">
                        <h4 className="abs-info-title">Как считаются пропуски?</h4>
                        <p className="abs-info-intro">Пропуски делятся на два типа:</p>
                        <ul className="abs-info-list">
                          <li><strong>По уважительной причине</strong> — болезнь, участие в мероприятиях, семейные обстоятельства и др.</li>
                          <li><strong>Без уважительной причины</strong> — отсутствие на занятии без предупреждения и подтверждения.</li>
                        </ul>
                      </article>
                    </aside>
                  </div>
                </div>
              )}
              {effectiveStudentTab === "group" && (() => {
                const totalStud = myGroupMembers.length;
                const starostaCount = starostaName ? 1 : 0;
                const visibleMembers = myGroupMembers.slice(0, groupMembersPage * GROUP_MEMBERS_PAGE_SIZE);
                const hasMoreMembers = myGroupMembers.length > visibleMembers.length;
                return (
                  <div className="group-page edu-group-page">
                    <header className="group-page-header">
                      <div className="group-page-title-block">
                        <div className="group-page-icon" aria-hidden><ActionIcon name="group" /></div>
                        <div>
                          <h2 className="group-page-title">Состав группы</h2>
                          <p className="group-page-sub">Группа: <strong>{myStudent?.groupNumber || "—"}</strong></p>
                          <p className="group-page-curator">Куратор группы: <strong className="group-curator-name">{groupCurator}</strong></p>
                        </div>
                      </div>
                      <div className="group-page-stats">
                        <article className="group-mini-stat">
                          <span className="group-mini-ico" aria-hidden><ActionIcon name="group" /></span>
                          <div>
                            <span className="group-mini-label">Всего студентов</span>
                            <strong className="group-mini-val">{totalStud}</strong>
                          </div>
                        </article>
                        <article className="group-mini-stat group-mini-stat--star">
                          <span className="group-mini-ico" aria-hidden><ActionIcon name="grade" /></span>
                          <div>
                            <span className="group-mini-label">Староста группы</span>
                            <strong className="group-mini-val">{starostaCount}</strong>
                          </div>
                        </article>
                      </div>
                    </header>
                    <div className="group-table-card edu-widget">
                      <table className="group-roster-table">
                        <thead>
                          <tr>
                            <th>#</th>
                            <th>ФИО студента</th>
                            <th>Роль</th>
                          </tr>
                        </thead>
                        <tbody>
                          {visibleMembers.map((member, idx) => {
                            const peer = students.find((s) => UiUtils.fullName(s) === member);
                            const peerId = peer?.id;
                            const isStar = member === starostaName;
                            return (
                              <tr key={`${member}-${idx}`}>
                                <td className="group-roster-num">{idx + 1}</td>
                                <td>
                                  <button
                                    type="button"
                                    className="group-roster-student"
                                    onClick={() => peer && setStudentPreviewName(member)}
                                    disabled={!peer}
                                  >
                                    <span className="group-roster-avatar">
                                      <img src={peerId ? (avatars[peerId] || "") : ""} alt="" />
                                    </span>
                                    <span className="group-roster-name">{member}</span>
                                  </button>
                                </td>
                                <td>
                                  {isStar ? (
                                    <span className="group-role-pill group-role-pill--star">Староста</span>
                                  ) : (
                                    <span className="group-role-pill group-role-pill--stud">Студент</span>
                                  )}
                                </td>
                              </tr>
                            );
                          })}
                        </tbody>
                      </table>
                      {myGroupMembers.length === 0 ? <p className="edu-widget-empty group-roster-empty">В группе пока нет студентов в списке.</p> : null}
                      {hasMoreMembers ? (
                        <button type="button" className="group-show-more" onClick={() => setGroupMembersPage((p) => p + 1)}>
                          Показать ещё <span className="group-show-more-chevron" aria-hidden>▼</span>
                        </button>
                      ) : null}
                    </div>
                  </div>
                );
              })()}
              {effectiveStudentTab === "settings" && (
                <div className="student-tab-page">
                  <header className="student-tab-page-head">
                    <h2 className="student-tab-page-title">Настройки</h2>
                    <p className="student-tab-page-sub">Контакты, дата рождения и смена пароля.</p>
                  </header>
                  <div className="student-settings-stack">
                    <article className="edu-widget student-settings-widget">
                      <div className="edu-widget-head">
                        <h3 className="edu-widget-title">Контакты и дата рождения</h3>
                      </div>
                      <div className="student-settings-fields">
                        <label className="student-settings-label">
                          <span>Телефон</span>
                          <input placeholder="+375-25-501-23-91" value={settingsForm.phone} onChange={(e) => setSettingsForm((p) => ({ ...p, phone: formatBelarusPhone(e.target.value) }))} />
                        </label>
                        <label className="student-settings-label">
                          <span>Email</span>
                          <input value={settingsForm.email} onChange={(e) => setSettingsForm((p) => ({ ...p, email: e.target.value }))} />
                        </label>
                        <label className="student-settings-label">
                          <span>Дата рождения</span>
                          <input
                            type="date"
                            value={settingsForm.birthDate}
                            disabled={Boolean(studentProfiles[session.studentName]?.birthDate)}
                            onChange={(e) => setSettingsForm((p) => ({ ...p, birthDate: e.target.value }))}
                          />
                        </label>
                      </div>
                      {studentProfiles[session.studentName]?.birthDate ? (
                        <p className="sub student-settings-hint">Дата рождения сохранена и больше не меняется.</p>
                      ) : (
                        <p className="sub student-settings-hint">После первого сохранения дату рождения изменить нельзя.</p>
                      )}
                      <button type="button" className="btn-main student-settings-save" onClick={updateProfileSettings}>Сохранить</button>
                    </article>
                    <article className="edu-widget student-settings-widget">
                      <div className="edu-widget-head">
                        <h3 className="edu-widget-title">Смена пароля</h3>
                      </div>
                      <div className="student-settings-fields">
                        <label className="student-settings-label">
                          <span>Текущий пароль</span>
                          <input type="text" className="password-manual-input" name="student-manual-old-password" autoComplete="off" data-lpignore="true" value={passwordForm.oldPassword} onChange={(e) => setPasswordForm((p) => ({ ...p, oldPassword: e.target.value }))} />
                        </label>
                        <label className="student-settings-label">
                          <span>Новый пароль</span>
                          <input type="password" autoComplete="new-password" value={passwordForm.newPassword} onChange={(e) => setPasswordForm((p) => ({ ...p, newPassword: e.target.value }))} />
                        </label>
                        <label className="student-settings-label">
                          <span>Подтверждение</span>
                          <input type="password" autoComplete="new-password" value={passwordForm.confirmPassword} onChange={(e) => setPasswordForm((p) => ({ ...p, confirmPassword: e.target.value }))} />
                        </label>
                      </div>
                      <p className="sub student-settings-hint">Минимум 8 символов: латиница, прописные и обычные буквы, цифры.</p>
                      <button type="button" className="btn-main student-settings-save" onClick={updatePassword}>Обновить пароль</button>
                    </article>
                  </div>
                  {settingsMessage ? <p className="student-settings-message">{settingsMessage}</p> : null}
                </div>
              )}
              </div>
            </div>
            {editingPostId !== null && (
              <div className="note-modal-backdrop" onClick={() => setEditingPostId(null)}>
                <div className="note-modal" onClick={(e) => e.stopPropagation()}>
                  <h4>Изменить заметку</h4>
                  <textarea
                    className="note-modal-input"
                    value={editingPostText}
                    onChange={(e) => setEditingPostText(e.target.value)}
                  />
                  <div className="note-modal-actions">
                    <button type="button" className="btn-main" onClick={() => saveEditPost(editingPostId)}>Сохранить</button>
                    <button type="button" className="btn-ghost" onClick={() => setEditingPostId(null)}>Отмена</button>
                  </div>
                </div>
              </div>
            )}
            {showNewNoteModal && (
              <div className="note-modal-backdrop" onClick={() => setShowNewNoteModal(false)}>
                <div className="note-modal" onClick={(e) => e.stopPropagation()}>
                  <h4>Новая заметка</h4>
                  <textarea
                    className="note-modal-input"
                    value={newNoteText}
                    onChange={(e) => setNewNoteText(e.target.value)}
                    onKeyDown={(e) => {
                      if (e.key === "Enter" && !e.shiftKey) {
                        e.preventDefault();
                        publishPost();
                      }
                    }}
                  />
                  <div className="note-modal-actions">
                    <button type="button" className="btn-main" onClick={publishPost}>Сохранить</button>
                    <button type="button" className="btn-ghost" onClick={() => setShowNewNoteModal(false)}>Отмена</button>
                  </div>
                </div>
              </div>
            )}
            {teacherPreviewName && previewTeacherProfile && (
              <div className="note-modal-backdrop" onClick={() => setTeacherPreviewName(null)}>
                <div className="teacher-profile-modal" onClick={(e) => e.stopPropagation()}>
                  <h3>{teacherPreviewName}</h3>
                  <div className="teacher-preview-avatar-wrap">
                    <img className="teacher-preview-avatar" src={previewTeacherProfile.avatar || ""} alt="" />
                  </div>
                  <div className="teacher-profile-grid">
                    <div>
                      <h4>Ученая степень</h4>
                      <p>{previewTeacherProfile.degree || "Не указано"}</p>
                      <h4>Должность, место работы</h4>
                      <p>{previewTeacherProfile.position || "Не указано"}</p>
                      <p>{previewTeacherProfile.department || "Не указано"}</p>
                    </div>
                    <div>
                      <h4>Контакты</h4>
                      <p>Почта: {previewTeacherProfile.email || "Не указана"}</p>
                      <p>Рабочий телефон: {previewTeacherProfile.workPhone ? formatBelarusPhone(previewTeacherProfile.workPhone) : "Не указан"}</p>
                      <p>{previewTeacherProfile.office || "Кабинет не указан"}</p>
                    </div>
                  </div>
                  <h4>Читаемые курсы</h4>
                  <p>{previewTeacherProfile.courses || "Не указано"}</p>
                </div>
              </div>
            )}
            {renderStudentPeekModal()}
          </main>
        </div>
      </div>
    );
  }

  if (session.role === "teacher") {
    const teacherNav = [
      ["profile", "home", "Главная"],
      ["schedule", "calendar", "Расписание"],
      ["journal", "journal", "Журнал"],
      ["notes", "notes", "Заметки"]
    ];
    const openTeacherJournalQuick = (group, discipline, lessonType = "") => {
      setTeacherJournalContext((prev) => ({
        ...prev,
        groupNumber: group,
        disciplineName: discipline,
        lessonType: normalizeLessonType(lessonType, 0)
      }));
      setTeacherTab("journal");
    };
    const ruStudentsLabel = (n) => {
      const m = n % 100;
      if (m >= 11 && m <= 14) return `${n} студентов`;
      const k = n % 10;
      if (k === 1) return `${n} студент`;
      if (k >= 2 && k <= 4) return `${n} студента`;
      return `${n} студентов`;
    };
    const journalRingClass = (avgNum) => {
      if (avgNum == null || Number.isNaN(avgNum)) return "tg-ring--muted";
      if (avgNum >= 4.25) return "tg-ring--hi";
      if (avgNum >= 4.1) return "tg-ring--mid";
      return "tg-ring--low";
    };
    const teacherLessonStatusRu = (st) => {
      if (st === "completed") return "Завершено";
      if (st === "current") return "Сейчас";
      return "Предстоит";
    };
    const journalValueTone = (value) => {
      if (String(value).includes("уваж")) return "excused";
      if (String(value).toUpperCase() === "Н") return "miss";
      const nums = gradeNumbers(value);
      const fallback = parseFloat(String(value || "").replace(",", "."));
      const n = nums.length ? nums.reduce((sum, item) => sum + item, 0) / nums.length : fallback;
      if (Number.isNaN(n)) return "empty";
      if (n >= 8) return "good";
      if (n >= 5) return "warn";
      return "bad";
    };
    const journalDateLabel = (iso, shiftWeeks = 0) => {
      const base = iso ? new Date(`${iso}T12:00:00`) : new Date();
      base.setDate(base.getDate() - (shiftWeeks * 7));
      return base.toLocaleDateString("ru-RU", { day: "2-digit", month: "2-digit" });
    };
    const moveTeacherJournalFocus = (rowIndex) => {
      const next = document.querySelector(`[data-teacher-journal-row="${rowIndex}"]`);
      if (next) {
        next.focus();
        next.select?.();
      }
    };

    return (
      <div className={`app-layout student-app-layout edu-role-layout student-theme-${studentTheme}`}>
        <aside className="sidebar student-sidebar edu-sidebar student-sidebar--collapse">
          <div className="brand edu-brand"><span className="edu-brand-full">EduFlow</span><span className="edu-brand-short" aria-hidden>E</span></div>
          <nav className="menu student-side-menu">
            {teacherNav.map(([id, icon, label]) => (
              <button key={id} type="button" className={teacherTab === id ? "active" : ""} onClick={() => setTeacherTab(id)}>
                <span className="menu-icon" aria-hidden><SidebarIcon name={icon} /></span>
                <span className="menu-label">{label}</span>
              </button>
            ))}
          </nav>
          <div className="student-sidebar-foot">
            <button type="button" className={teacherTab === "settings" ? "active student-side-aux" : "student-side-aux"} onClick={() => {
              setPasswordForm({ oldPassword: "", newPassword: "", confirmPassword: "" });
              setSettingsMessage("");
              setTeacherTab("settings");
            }}>
              <span className="menu-icon" aria-hidden><SidebarIcon name="settings" /></span>
              <span className="menu-label">Настройки</span>
            </button>
            <button type="button" className="student-side-aux" onClick={logout}>
              <span className="menu-icon" aria-hidden><SidebarIcon name="logout" /></span>
              <span className="menu-label">Выход</span>
            </button>
          </div>
        </aside>
        <div className={`student-shell edu-student-shell edu-student-shell--template sakura-bg student-theme-${studentTheme}`}>
          <div className="sakura-layer" aria-hidden="true">
            {Array.from({ length: 8 }).map((_, idx) => <span key={idx} className="petal" style={{ "--d": `${idx * 1.2}s`, "--x": `${(idx * 7) % 100}%`, "--s": `${8 + (idx % 6)}s` }} />)}
          </div>
          <main className="main student-dashboard-main">
            <header className="edu-topbar edu-topbar--teacher">
              <div className="edu-topbar-title-slot">
                <span className="edu-topbar-crumb">EduFlow</span>
              </div>
              <div className="edu-topbar-right">
                <button type="button" className="edu-top-user" onClick={() => setTeacherTab("profile")} title="Перейти в профиль">
                  <span className="edu-top-avatar-wrap">
                    <img className="edu-top-avatar" src={activeTeacherProfile?.avatar || ""} alt="" />
                  </span>
                  <span className="edu-top-user-text">
                    <span className="edu-top-user-name">{shortStudentName(session.teacherName)}</span>
                    <span className="edu-top-user-role">Преподаватель</span>
                  </span>
                </button>
              </div>
            </header>
            <div className="student-dashboard-body edu-teacher-body">
            {teacherTab === "profile" && (
              <TeacherProfile
                activeTeacherProfile={activeTeacherProfile}
                journalRingClass={journalRingClass}
                onOpenJournal={openTeacherJournalQuick}
                onOpenProfileEditor={openTeacherProfileEditor}
                ruStudentsLabel={ruStudentsLabel}
                rusWeekday={rusWeekday}
                session={session}
                setTeacherTab={setTeacherTab}
                teacherDashboardGroups={teacherDashboardGroups}
                teacherJournalRows={teacherQuickJournalRows}
                teacherLessonStatusRu={teacherLessonStatusRu}
                teacherScheduleWithStatus={teacherScheduleWithStatus}
                teacherTheme={studentTheme}
                onChangeTeacherTheme={setStudentTheme}
              />
            )}

            {teacherTab === "schedule" && <section className="card">
              <h3>Расписание</h3>
              <div className="row student-schedule-search">
                <input className="student-schedule-search-input" placeholder="Поиск расписания" value={scheduleSearch} onChange={(e) => setScheduleSearch(e.target.value)} />
              </div>
              <div className="admin-schedule-layout teacher-schedule-layout">
                <section className="admin-schedule-board">
                  <div className="admin-calendar-grid admin-calendar-grid--six role-schedule-grid">
                    <div className="admin-calendar-corner" />
                    {scheduleDays.map((day) => <div key={day} className="admin-calendar-head">{dayShortName(day)}<span>{day} · {lessonDateByDay[day] || ""}</span></div>)}
                    {lessonSlots.map((slot) => (
                      <Fragment key={slot.number}>
                        <div className="admin-calendar-time">{slot.start}<span>{slot.end}</span></div>
                        {scheduleDays.map((day) => {
                          const lessons = searchableTeacherScheduleByDay.find((block) => block.day === day)?.lessons.filter((lesson) => Number(lesson.slot) === Number(slot.number)) || [];
                          return (
                            <div key={`${day}-${slot.number}`} className="admin-calendar-cell">
                              {lessons.map((s, idx) => {
                                const hasGrades = grades.some((grade) => grade.disciplineName === s.discipline && (groups.find((g) => String(g.number) === String(s.group))?.students || []).includes(grade.studentName));
                                return (
                                  <button type="button" className={`admin-lesson-card color-${idx % 5}`} key={`${day}-${slot.number}-${s.discipline}-${s.lessonType}`} onClick={() => fillTeacherLessonForm(s)}>
                                    <strong className="schedule-discipline-name" title={disciplineFullName(s.discipline)}>{s.discipline}</strong>
                                    <span>{s.typeMeta?.label || s.lessonType} · гр. {s.group}</span>
                                    <small>ауд. {s.room}</small>
                                    <small>{hasGrades ? "Есть оценки" : "Не заполнено"}</small>
                                  </button>
                                );
                              })}
                              {lessons.length === 0 && <button type="button" className="calendar-add" onClick={() => startTeacherLessonAt(day, slot.number)}>+</button>}
                            </div>
                          );
                        })}
                      </Fragment>
                    ))}
                  </div>
                </section>
                <aside className="admin-schedule-side">
                  <section className="card admin-side-editor">
                    <h3>{teacherLessonForm.editId || teacherLessonForm.replacesKey ? "Изменить пару" : "Добавить пару"}</h3>
                    <label>День<select value={teacherLessonForm.day} onChange={(e) => setTeacherLessonForm((p) => ({ ...p, day: e.target.value }))}>{scheduleDays.map((d) => <option key={d} value={d}>{d}</option>)}</select></label>
                    <label>Пара<select value={teacherLessonForm.slot} onChange={(e) => setTeacherLessonForm((p) => ({ ...p, slot: Number(e.target.value) }))}>{lessonSlots.map((s) => <option key={s.number} value={s.number}>{`${s.number} пара (${s.start})`}</option>)}</select></label>
                    <label>Тип занятия<select value={teacherLessonForm.lessonType} onChange={(e) => setTeacherLessonForm((p) => ({ ...p, lessonType: e.target.value }))}>{LESSON_TYPES.map((type) => <option key={type.code} value={type.code}>{type.title}</option>)}</select></label>
                    <label>Предмет<select value={teacherLessonForm.discipline} onChange={(e) => setTeacherLessonForm((p) => ({ ...p, discipline: e.target.value }))}><option value="">Дисциплина</option>{teacherDisciplines.map((d) => <option key={d.id} value={d.name}>{d.name}</option>)}</select></label>
                    <label>Группа<select value={teacherLessonForm.group} onChange={(e) => setTeacherLessonForm((p) => ({ ...p, group: e.target.value }))}><option value="">Группа</option>{groups.map((g) => <option key={g.id} value={g.number}>{g.number}</option>)}</select></label>
                    <label>Аудитория<input value={teacherLessonForm.room} onChange={(e) => setTeacherLessonForm((p) => ({ ...p, room: e.target.value }))} /></label>
                    <button type="button" className="btn-main" onClick={addTeacherLesson}>Сохранить пару</button>
                    {(teacherLessonForm.editId || teacherLessonForm.replacesKey) && <button type="button" className="btn-danger" onClick={deleteTeacherLesson}>Удалить пару</button>}
                  </section>
                </aside>
              </div>
            </section>}

            {teacherTab === "notes" && (
              <div className="student-tab-page">
                <header className="student-tab-page-head student-tab-page-head--row">
                  <div>
                    <h2 className="student-tab-page-title">Заметки</h2>
                  </div>
                </header>
                <article className="edu-widget student-tab-panel student-panel-unified">
                  <div className="notes-board notes-board--tab">
                    {myFeed.length === 0 && <p className="edu-widget-empty">Пока нет заметок.</p>}
                    {myFeed.map((post, idx) => (
                      <article
                        className={`note-sticky note-variant-${idx % 4}`}
                        key={post.id}
                        onClick={() => startEditPost(post)}
                        onKeyDown={(e) => { if (e.key === "Enter") startEditPost(post); }}
                        role="button"
                        tabIndex={0}
                      >
                        <p className="note-text">{post.text}</p>
                        <div className="post-menu-wrap">
                          <button
                            type="button"
                            className="post-menu-button"
                            onClick={(e) => {
                              e.stopPropagation();
                              setOpenPostMenuId((prev) => prev === post.id ? null : post.id);
                            }}
                          >
                            ...
                          </button>
                          {openPostMenuId === post.id && (
                            <div className="post-menu-dropdown">
                              <button type="button" className="btn-ghost" onClick={(e) => { e.stopPropagation(); startEditPost(post); }} title="Редактировать" aria-label="Редактировать запись">Редактировать</button>
                              <button type="button" className="btn-ghost" onClick={(e) => { e.stopPropagation(); removePost(post.id); }} title="Удалить" aria-label="Удалить запись">Удалить</button>
                            </div>
                          )}
                        </div>
                        <div className="note-date note-date-bottom"><span>{post.createdAt}</span></div>
                      </article>
                    ))}
                  </div>
                </article>
                <button type="button" className="edu-floating-add-note" onClick={() => setShowNewNoteModal(true)} aria-label="Добавить заметку">
                  <span className="edu-floating-add-note-plus">+</span>
                  <span className="edu-floating-add-note-text">Добавить заметку</span>
                </button>
              </div>
            )}

            {teacherTab === "journal" && (
              <div className="teacher-journal-page">
                <header className="student-tab-page-head">
                  <h2 className="student-tab-page-title">Журнал</h2>
                </header>

                <article className="edu-widget teacher-journal-context">
                  <div className="teacher-journal-toolbar">
                    <div className="row teacher-journal-context-row">
                      <label className="teacher-journal-label">
                        Группа
                        <select
                          value={teacherJournalContext.groupNumber}
                          onChange={(e) => {
                            const v = e.target.value;
                            setTeacherJournalPage(1);
                            setTeacherJournalContext((c) => {
                              const next = { ...c, groupNumber: v };
                              const opts = [...new Set(teacherSchedule.filter((l) => String(l.group) === String(v)).map((l) => l.discipline))];
                              if (opts.length === 0) {
                                next.disciplineName = "";
                              } else if (!c.disciplineName || !opts.includes(c.disciplineName)) {
                                next.disciplineName = opts[0];
                              }
                              return next;
                            });
                          }}
                        >
                          <option value="">Выберите группу</option>
                          {teacherGroups.map((g) => <option key={g} value={g}>{g}</option>)}
                        </select>
                      </label>
                      <label className="teacher-journal-label">
                        Режим
                        <select value={teacherJournalMode} onChange={(e) => { setTeacherJournalMode(e.target.value); setTeacherJournalPage(1); }}>
                          <option value="grade">Выставить оценку</option>
                          <option value="absence">Отметить пропуск</option>
                        </select>
                      </label>
                      <label className="teacher-journal-label">
                        Предмет
                        <select value={teacherJournalContext.disciplineName} onChange={(e) => { setTeacherJournalContext((c) => ({ ...c, disciplineName: e.target.value })); setTeacherJournalPage(1); }}>
                          <option value="">Выберите предмет</option>
                          {teacherJournalDisciplineOptions.map((name) => <option key={name} value={name}>{name}</option>)}
                        </select>
                      </label>
                      <label className="teacher-journal-label">
                        Тип занятия
                        <select value={teacherJournalContext.lessonType} onChange={(e) => { setTeacherJournalContext((c) => ({ ...c, lessonType: e.target.value })); setTeacherJournalPage(1); }}>
                          {teacherJournalLessonTypeOptions.map((typeCode) => {
                            const meta = lessonTypeMeta(typeCode);
                            return <option key={typeCode} value={typeCode}>{meta.title}</option>;
                          })}
                        </select>
                      </label>
                      <label className="teacher-journal-label">
                        Дата
                        <input type="date" value={teacherJournalContext.date} onChange={(e) => setTeacherJournalContext((c) => ({ ...c, date: e.target.value }))} />
                      </label>
                    </div>
                  </div>
                </article>

                <article className="edu-widget teacher-journal-board">
                  {!teacherJournalContext.groupNumber ? (
                    <p className="edu-widget-empty">Выберите группу — список студентов появится сразу.</p>
                  ) : (
                    <div className="teacher-journal-table-wrap">
                      <div className="teacher-journal-board-head">
                        <div>
                          <strong>{teacherJournalContext.groupNumber} — {teacherJournalContext.disciplineName || "Предмет не выбран"} — {lessonTypeMeta(teacherJournalContext.lessonType).title}</strong>
                          <span>{new Date(`${teacherJournalContext.date}T12:00:00`).toLocaleDateString("ru-RU")}</span>
                        </div>
                      </div>
                      <table className="teacher-journal-live-table teacher-journal-modern-table">
                        <thead>
                          <tr>
                            <th>№</th>
                            <th>Студент</th>
                            <th>{journalDateLabel(teacherJournalContext.date, 3)}</th>
                            <th>{journalDateLabel(teacherJournalContext.date, 2)}</th>
                            <th>{journalDateLabel(teacherJournalContext.date, 1)}</th>
                            <th>{new Date(`${teacherJournalContext.date}T12:00:00`).toLocaleDateString("ru-RU")}<span className="journal-today-mark">(Сегодня)</span></th>
                            <th>{teacherJournalMode === "absence" ? "Всего пропусков" : "Средний балл"}</th>
                          </tr>
                        </thead>
                        <tbody>
                          {paginate(teacherJournalStudents, teacherJournalPage).map((s, rowIdx) => {
                            const studentName = UiUtils.fullName(s);
                            const isAbsenceMode = teacherJournalMode === "absence";
                            const selectedType = normalizeLessonType(teacherJournalContext.lessonType);
                            const gradeRecords = grades.filter((g) => {
                              const meta = gradeMetaOf(g);
                              return g.studentName === studentName
                                && g.disciplineName === teacherJournalContext.disciplineName
                                && normalizeLessonType(g.lessonType || meta.lessonType) === selectedType;
                            });
                            const currentGradeRecords = gradeRecords.filter((grade) => gradeMetaOf(grade).date === teacherJournalContext.date);
                            const absenceRecords = absences.filter((a) => a.studentName === studentName
                              && a.disciplineName === teacherJournalContext.disciplineName
                              && normalizeLessonType(a.lessonType) === selectedType);
                            const currentAbsenceRecord = absenceRecords.find((a) => a.date === teacherJournalContext.date);
                            const hasDraft = Object.prototype.hasOwnProperty.call(teacherJournalDrafts, studentName);
                            const currentDraft = teacherJournalDrafts[studentName] ?? "";
                            const currentAbsenceExcused = currentAbsenceRecord && (hasExcuseReason(currentAbsenceRecord.reason) || Number(currentAbsenceRecord.excusedHours || 0) > 0);
                            const currentInputValue = isAbsenceMode
                              ? (hasDraft ? currentDraft : (currentAbsenceRecord ? String((Number(currentAbsenceRecord.count || 0) * ACADEMIC_HOURS_PER_ABSENCE) || Number(currentAbsenceRecord.excusedHours || 0)) : ""))
                              : (hasDraft ? currentDraft : currentGradeRecords.map((grade) => grade.value).filter(Boolean).join(", "));
                            const totalAbsenceHours = absenceRecords.reduce((sum, item) => sum + (Number(item.count || 0) * ACADEMIC_HOURS_PER_ABSENCE), 0);
                            const avgValue = isAbsenceMode ? `${totalAbsenceHours} ч` : (gradeRecords.length ? averageValue(gradeRecords, (entry) => entry.value) : "—");
                            const historyDates = [3, 2, 1].map((shift) => {
                              const d = new Date(`${teacherJournalContext.date}T12:00:00`);
                              d.setDate(d.getDate() - (shift * 7));
                              return d.toISOString().slice(0, 10);
                            });
                            const historyValues = historyDates.map((date) => {
                              if (isAbsenceMode) {
                                const rows = absenceRecords.filter((item) => item.date === date);
                                const hours = rows.reduce((sum, item) => sum + (Number(item.count || 0) * ACADEMIC_HOURS_PER_ABSENCE) + Number(item.excusedHours || 0), 0);
                                const excused = rows.some((item) => hasExcuseReason(item.reason) || Number(item.excusedHours || 0) > 0);
                                return hours ? `${hours} ч${excused ? " уваж" : ""}` : "";
                              }
                              return gradeRecords
                                .filter((grade) => gradeMetaOf(grade).date === date)
                                .map((grade) => grade.value)
                                .filter(Boolean)
                                .join(", ");
                            });
                            return (
                              <tr key={s.id}>
                                <td className="teacher-journal-row-num">{((teacherJournalPage - 1) * PAGE_SIZE) + rowIdx + 1}</td>
                                <td>
                                  <span className="teacher-journal-student-cell">
                                    <span className="teacher-journal-avatar"><img src={avatars[s.id] || ""} alt="" /></span>
                                    {studentName}
                                  </span>
                                </td>
                                {historyValues.map((value, idx) => (
                                  <td key={`${studentName}-hist-${idx}`}>
                                    {value ? <span className={`journal-grade-chip journal-grade-chip--${journalValueTone(value)}`}>{String(value).replace(" уваж", "")}</span> : null}
                                  </td>
                                ))}
                                <td>
                                  <input
                                    className={`teacher-journal-select teacher-journal-select--${currentAbsenceExcused ? "excused" : journalValueTone(currentInputValue)}`}
                                    data-teacher-journal-row={((teacherJournalPage - 1) * PAGE_SIZE) + rowIdx}
                                    value={currentInputValue}
                                    onChange={(e) => {
                                      const value = e.target.value;
                                      if (String(value).trim().toUpperCase() === "Н") setTeacherJournalMode("absence");
                                      setTeacherJournalDrafts((prev) => ({ ...prev, [studentName]: value }));
                                    }}
                                    onKeyDown={(e) => {
                                      if (e.key === "Enter") {
                                        e.preventDefault();
                                        saveTeacherJournalRow(studentName, e.currentTarget.value);
                                      } else if (e.key === "ArrowDown" || e.key === "ArrowUp") {
                                        e.preventDefault();
                                        const currentRow = Number(e.currentTarget.dataset.teacherJournalRow);
                                        moveTeacherJournalFocus(currentRow + (e.key === "ArrowDown" ? 1 : -1));
                                      }
                                    }}
                                    onBlur={(e) => {
                                      if (Object.prototype.hasOwnProperty.call(teacherJournalDrafts, studentName)) saveTeacherJournalRow(studentName, e.currentTarget.value);
                                    }}
                                  />
                                </td>
                                <td><span className={`teacher-journal-avg teacher-journal-avg--${journalValueTone(avgValue)}`}>{avgValue}</span></td>
                              </tr>
                            );
                          })}
                          {teacherJournalStudents.length === 0 && (
                            <tr>
                              <td colSpan={7}>В выбранной группе пока нет студентов.</td>
                            </tr>
                          )}
                        </tbody>
                      </table>
                      {renderPager(teacherJournalStudents.length, teacherJournalPage, setTeacherJournalPage)}
                    </div>
                  )}
                </article>
              </div>
            )}
            {teacherTab === "settings" && (
              <div className="student-tab-page">
                <header className="student-tab-page-head">
                  <h2 className="student-tab-page-title">Настройки</h2>
                  <p className="student-tab-page-sub">Смена пароля преподавателя.</p>
                </header>
                <div className="student-settings-stack student-settings-stack--single">
                  <article className="edu-widget student-settings-widget">
                    <div className="edu-widget-head">
                      <h3 className="edu-widget-title">Смена пароля</h3>
                    </div>
                    <div className="student-settings-fields">
                      <label className="student-settings-label">
                        <span>Текущий пароль</span>
                        <input type="text" className="password-manual-input" name="teacher-manual-old-password" autoComplete="off" data-lpignore="true" value={passwordForm.oldPassword} onChange={(e) => setPasswordForm((p) => ({ ...p, oldPassword: e.target.value }))} />
                      </label>
                      <label className="student-settings-label">
                        <span>Новый пароль</span>
                        <input type="password" autoComplete="new-password" value={passwordForm.newPassword} onChange={(e) => setPasswordForm((p) => ({ ...p, newPassword: e.target.value }))} />
                      </label>
                      <label className="student-settings-label">
                        <span>Подтверждение</span>
                        <input type="password" autoComplete="new-password" value={passwordForm.confirmPassword} onChange={(e) => setPasswordForm((p) => ({ ...p, confirmPassword: e.target.value }))} />
                      </label>
                    </div>
                    <p className="sub student-settings-hint">Минимум 8 символов: латиница, прописные и обычные буквы, цифры.</p>
                    <button type="button" className="btn-main student-settings-save" onClick={updatePassword}>Обновить пароль</button>
                  </article>
                </div>
                {settingsMessage ? <p className="student-settings-message">{settingsMessage}</p> : null}
              </div>
            )}
            {editingPostId !== null && (
              <div className="note-modal-backdrop" onClick={() => setEditingPostId(null)}>
                <div className="note-modal" onClick={(e) => e.stopPropagation()}>
                  <h4>Изменить заметку</h4>
                  <textarea className="note-modal-input" value={editingPostText} onChange={(e) => setEditingPostText(e.target.value)} />
                  <div className="note-modal-actions">
                    <button type="button" className="btn-main" onClick={() => saveEditPost(editingPostId)}>Сохранить</button>
                    <button type="button" className="btn-ghost" onClick={() => setEditingPostId(null)}>Отмена</button>
                  </div>
                </div>
              </div>
            )}
            {showNewNoteModal && (
              <div className="note-modal-backdrop" onClick={() => setShowNewNoteModal(false)}>
                <div className="note-modal" onClick={(e) => e.stopPropagation()}>
                  <h4>Новая заметка</h4>
                  <textarea
                    className="note-modal-input"
                    value={newNoteText}
                    onChange={(e) => setNewNoteText(e.target.value)}
                    onKeyDown={(e) => {
                      if (e.key === "Enter" && !e.shiftKey) {
                        e.preventDefault();
                        publishPost();
                      }
                    }}
                  />
                  <div className="note-modal-actions">
                    <button type="button" className="btn-main" onClick={publishPost}>Сохранить</button>
                    <button type="button" className="btn-ghost" onClick={() => setShowNewNoteModal(false)}>Отмена</button>
                  </div>
                </div>
              </div>
            )}
            {teacherHomeEditOpen && teacherProfileDraft && (
              <div className="note-modal-backdrop" onClick={closeTeacherProfileEditor}>
                <div className="teacher-profile-edit-modal" onClick={(e) => e.stopPropagation()}>
                  <h3>Данные профиля преподавателя</h3>
                  <label className="teacher-avatar-edit-field">
                    <span>Фото профиля</span>
                    <input type="file" accept="image/*" onChange={(e) => e.target.files?.[0] && updateTeacherDraftAvatar(e.target.files[0])} />
                  </label>
                  <div className="teacher-profile-grid">
                    <div>
                      <h4>Ученая степень</h4>
                      <input value={teacherProfileDraft.degree || ""} onChange={(e) => updateTeacherDraftField("degree", e.target.value)} placeholder="Ученая степень, звание" />
                      <h4>Должность</h4>
                      <input value={teacherProfileDraft.position || ""} onChange={(e) => updateTeacherDraftField("position", e.target.value)} placeholder="Должность" />
                      <h4>Кафедра</h4>
                      <input value={teacherProfileDraft.department || ""} onChange={(e) => updateTeacherDraftField("department", e.target.value)} placeholder="Кафедра" />
                      <h4>Стаж</h4>
                      <input value={teacherProfileDraft.experienceYears || ""} onChange={(e) => updateTeacherDraftField("experienceYears", e.target.value)} placeholder="например, 7 лет" />
                      <h4>Текущий статус</h4>
                      <select value={teacherProfileDraft.workStatus || "Активен"} onChange={(e) => updateTeacherDraftField("workStatus", e.target.value)}>
                        {TEACHER_WORK_STATUSES.map((status) => <option key={status} value={status}>{status}</option>)}
                      </select>
                    </div>
                    <div>
                      <h4>Контакты</h4>
                      <input value={teacherProfileDraft.email || ""} onChange={(e) => updateTeacherDraftField("email", e.target.value)} placeholder="Почта" />
                      <input value={teacherProfileDraft.workPhone || ""} onChange={(e) => updateTeacherDraftField("workPhone", formatBelarusPhone(e.target.value))} placeholder="+375-25-501-23-91" />
                      <input value={teacherProfileDraft.office || ""} onChange={(e) => updateTeacherDraftField("office", e.target.value)} placeholder="Кабинет" />
                    </div>
                  </div>
                  <h4>Ссылки и курсы</h4>
                  <textarea className="note-modal-input" value={teacherProfileDraft.links || ""} onChange={(e) => updateTeacherDraftField("links", e.target.value)} placeholder="Ссылки" />
                  <textarea className="note-modal-input" value={teacherProfileDraft.courses || ""} onChange={(e) => updateTeacherDraftField("courses", e.target.value)} placeholder="Читаемые курсы" />
                  <div className="note-modal-actions">
                    <button type="button" className="btn-main" onClick={saveTeacherProfileDraft}>Сохранить</button>
                    <button type="button" className="btn-ghost" onClick={closeTeacherProfileEditor}>Отмена</button>
                  </div>
                </div>
              </div>
            )}
            {renderStudentPeekModal()}
            {teacherDisciplineModal && (
              <div className="note-modal-backdrop" role="dialog" aria-modal="true">
                <div className="note-modal teacher-discipline-modal">
                  <h3>Дисциплины преподавателя</h3>
                  <p className="sub">{teacherDisciplineModal.teacherName || "Выберите преподавателя"}</p>
                  <label>Преподаватель<select value={teacherDisciplineModal.teacherName} onChange={(e) => setTeacherDisciplineModal((p) => ({ ...p, teacherName: e.target.value }))}><option value="">Преподаватель</option>{teachers.map((t) => <option key={t.id} value={UiUtils.fullName(t)}>{UiUtils.fullName(t)}</option>)}</select></label>
                  <label>Факультет<select value={teacherDisciplineModal.faculty} onChange={(e) => {
                    const faculty = e.target.value;
                    setTeacherDisciplineModal((p) => ({ ...p, faculty, specialty: specialtiesCatalog[faculty]?.[0] || "", disciplineName: "" }));
                  }}><option value="">Факультет</option>{facultiesCatalog.map((faculty) => <option key={faculty} value={faculty} title={facultyFullName(faculty)}>{faculty}</option>)}</select></label>
                  <label>Специальность<select value={teacherDisciplineModal.specialty} onChange={(e) => setTeacherDisciplineModal((p) => ({ ...p, specialty: e.target.value, disciplineName: "" }))}><option value="">Специальность</option>{(specialtiesCatalog[teacherDisciplineModal.faculty] || []).map((specialty) => <option key={specialty} value={specialty}>{specialty}</option>)}</select></label>
                  <label>Курс<select value={teacherDisciplineModal.course || "1 курс"} onChange={(e) => setTeacherDisciplineModal((p) => ({ ...p, course: e.target.value }))}>{["1 курс", "2 курс", "3 курс", "4 курс"].map((course) => <option key={course} value={course}>{course}</option>)}</select></label>
                  <label>Дисциплина<select value={teacherDisciplineModal.disciplineName} onChange={(e) => {
                    const selected = disciplineRows.find((d) => d.name === e.target.value) || {};
                    setTeacherDisciplineModal((p) => ({ ...p, disciplineName: e.target.value, course: selected.course || p.course || "1 курс" }));
                  }}><option value="">Дисциплина</option>{disciplineRows.filter((d) => (!teacherDisciplineModal.faculty || d.faculty === teacherDisciplineModal.faculty) && (!teacherDisciplineModal.specialty || d.specialty === teacherDisciplineModal.specialty) && (!teacherDisciplineModal.course || d.course === teacherDisciplineModal.course)).map((d) => <option key={d.name} value={d.name}>{d.name}</option>)}</select></label>
                  <div className="note-modal-actions">
                    <button type="button" className="btn-main" onClick={assignTeacherDiscipline}>Сохранить</button>
                    <button type="button" className="btn-ghost" onClick={() => setTeacherDisciplineModal(null)}>Отмена</button>
                  </div>
                </div>
              </div>
            )}
            </div>
          </main>
        </div>
      </div>
    );
  }

  return (
    <div className="app-layout student-app-layout edu-role-layout">
      <aside className="sidebar student-sidebar edu-sidebar student-sidebar--collapse">
        <div className="brand edu-brand"><span className="edu-brand-full">EduFlow</span><span className="edu-brand-short" aria-hidden>E</span></div>
        <nav className="menu student-side-menu">
          {adminMenu.map((item) => (
            <button key={item.id} type="button" className={adminTab === item.id ? "active" : ""} onClick={() => setAdminTab(item.id)}>
              <span className="menu-icon" aria-hidden><SidebarIcon name={item.icon} /></span>
              <span className="menu-label">{item.label}</span>
            </button>
          ))}
        </nav>
        <div className="student-sidebar-foot">
          <button type="button" className="student-side-aux" onClick={logout}>
            <span className="menu-icon" aria-hidden><SidebarIcon name="logout" /></span>
            <span className="menu-label">Выход</span>
          </button>
        </div>
      </aside>
      <div className="student-shell edu-student-shell sakura-bg">
        <div className="sakura-layer" aria-hidden="true">
          {Array.from({ length: 8 }).map((_, idx) => <span key={idx} className="petal" style={{ "--d": `${idx * 1.2}s`, "--x": `${(idx * 7) % 100}%`, "--s": `${8 + (idx % 6)}s` }} />)}
        </div>
        <main className="main student-dashboard-main">
          <header className="edu-topbar edu-topbar--admin">
            <div className="edu-topbar-title-slot">
              <span className="edu-topbar-crumb">EduFlow</span>
            </div>
            <div className="edu-topbar-right">
              <button type="button" className="edu-top-user edu-top-user--admin" onClick={() => setAdminTab("dashboard")} title="Перейти в профиль администратора">
                <span className="edu-top-avatar-wrap edu-top-avatar-wrap--admin">
                  <span className="edu-top-avatar edu-top-avatar--letter">A</span>
                </span>
                <span className="edu-top-user-text">
                  <span className="edu-top-user-name">Администратор</span>
                  <span className="edu-top-user-role">Панель управления</span>
                </span>
              </button>
            </div>
          </header>
          <div className="student-dashboard-body edu-admin-body">
          {message && <p className="msg">{message}</p>}

          {adminTab === "dashboard" && (
            <div className="admin-new-dashboard">
              <section className="admin-new-kpis">
                <article className="admin-new-kpi"><span className="admin-new-kpi-title">Всего студентов</span><strong>{students.length}</strong></article>
                <article className="admin-new-kpi"><span className="admin-new-kpi-title">Преподавателей</span><strong>{teachers.length}</strong></article>
                <article className="admin-new-kpi"><span className="admin-new-kpi-title">Групп</span><strong>{groups.length}</strong></article>
                <article className="admin-new-kpi"><span className="admin-new-kpi-title">Предметов</span><strong>{disciplines.length}</strong></article>
              </section>

              <section className="admin-new-row">
                <article className="edu-widget admin-new-widget">
                  <div className="edu-widget-head"><h3 className="edu-widget-title">Статистика успеваемости</h3></div>
                  <div className="admin-perf-grid">
                    <div className="admin-perf-bars">
                      {[adminPerf.excellentPct, adminPerf.goodPct, adminPerf.satisfactoryPct, adminPerf.poorPct].map((pct, idx) => (
                        <div key={idx} className={`admin-perf-bar admin-perf-bar--${idx}`} style={{ "--h": `${Math.max(8, pct)}%` }} />
                      ))}
                    </div>
                    <ul className="admin-perf-legend">
                      <li>Отлично <strong>{adminPerf.excellentPct}%</strong></li>
                      <li>Хорошо <strong>{adminPerf.goodPct}%</strong></li>
                      <li>Удовлетворительно <strong>{adminPerf.satisfactoryPct}%</strong></li>
                      <li>Неудовлетворительно <strong>{adminPerf.poorPct}%</strong></li>
                    </ul>
                  </div>
                </article>

                <article className="edu-widget admin-new-widget">
                  <div className="edu-widget-head"><h3 className="edu-widget-title">Активность пользователей</h3></div>
                  <div className="admin-activity-grid">
                    <div className="admin-activity-donut" style={{ background: adminActivityDonut.bg }}>
                      <div className="admin-activity-hole">
                        <span>Всего</span>
                        <strong>{adminActivityDonut.total}</strong>
                      </div>
                    </div>
                    <ul className="admin-activity-legend">
                      <li><span className="admin-activity-dot admin-activity-dot--students" />Студенты: {adminActivityDonut.studentsCount}</li>
                      <li><span className="admin-activity-dot admin-activity-dot--teachers" />Преподаватели: {adminActivityDonut.teachersCount}</li>
                      <li><span className="admin-activity-dot admin-activity-dot--admins" />Администраторы: {adminActivityDonut.adminsCount}</li>
                    </ul>
                  </div>
                </article>
              </section>

              <section className="admin-new-row">
                <article className="edu-widget admin-new-widget">
                  <div className="edu-widget-head"><h3 className="edu-widget-title">Группы</h3></div>
                  <table className="admin-new-table">
                    <thead><tr><th>Название</th><th>Студентов</th><th>Куратор</th><th>Средний балл</th></tr></thead>
                    <tbody>
                      {adminGroupsPreview.map((row) => <tr key={row.id}><td>{row.name}</td><td>{row.students}</td><td>{row.curator}</td><td>{row.avg}</td></tr>)}
                    </tbody>
                  </table>
                </article>

                <article className="edu-widget admin-new-widget">
                  <div className="edu-widget-head"><h3 className="edu-widget-title">Быстрые действия</h3></div>
                  <div className="admin-quick-actions">
                    <button type="button" className="btn-ghost" onClick={() => setAdminTab("students")}>Добавить студента</button>
                    <button type="button" className="btn-ghost" onClick={() => setAdminTab("teachers")}>Добавить преподавателя</button>
                    <button type="button" className="btn-ghost" onClick={() => setAdminTab("groups")}>Создать группу</button>
                    <button type="button" className="btn-ghost" onClick={() => setAdminTab("disciplines")}>Добавить предмет</button>
                    <button type="button" className="btn-ghost" onClick={() => setAdminTab("schedule")}>Управление расписанием</button>
                  </div>
                </article>
              </section>

            </div>
          )}

          {adminTab === "students" && (
            <AdminStudentsSection
              allFaculties={allFaculties}
              adminFacultyFilter={adminFacultyFilter}
              setAdminFacultyFilter={setAdminFacultyFilter}
              adminSpecialtyFilter={adminSpecialtyFilter}
              setAdminSpecialtyFilter={setAdminSpecialtyFilter}
              adminGroupFilter={adminGroupFilter}
              setAdminGroupFilter={setAdminGroupFilter}
              specialtiesForFaculty={specialtiesForFaculty}
              groupsForFacultySpecialty={groupsForFacultySpecialty}
              adminGroupedStudents={adminGroupedStudents}
              adminGroupedPage={adminGroupedPage}
              setAdminGroupedPage={setAdminGroupedPage}
              paginate={paginate}
              UiUtils={UiUtils}
              studentMetaOf={studentMetaOf}
              avatars={avatars}
              setStudentPreviewName={setStudentPreviewName}
              renderPager={renderPager}
              adminAllStudents={adminAllStudents}
              adminStudentsPage={adminStudentsPage}
              setAdminStudentsPage={setAdminStudentsPage}
              avgMap={avgMap}
              stForm={stForm}
              fillStudentFormByName={fillStudentFormByName}
              students={students}
              setStForm={setStForm}
              STUDENT_STATUSES={STUDENT_STATUSES}
              groups={groups}
              disciplines={disciplines}
              selectedAdminGroupMembers={selectedAdminGroupMembers}
              saveStudent={saveStudent}
              deleteStudent={deleteStudent}
              setMessage={setMessage}
              onDeleteStudentByName={deleteStudentByName}
              onBulkMoveStudents={bulkMoveStudentsToGroup}
              onBulkSetStudentStatus={bulkSetStudentStatus}
              onBulkDeleteStudents={bulkDeleteStudents}
              onImportStudentsBatch={importStudentsBatch}
              searchTerm={search}
              onClearSearch={() => setSearch("")}
              groupMeta={groupMeta}
              onAssignStarosta={assignStarosta}
              facultiesCatalog={facultiesCatalog}
              specialtiesCatalog={specialtiesCatalog}
              onAddFaculty={addFacultyToCatalog}
              onAddSpecialty={addSpecialtyToCatalog}
            />
          )}

          {adminTab === "teachers" && (
            <AdminTeachersSection
              teachers={teachers}
              filtered={filtered}
              UiUtils={UiUtils}
              teacherProfileOf={teacherProfileOf}
              setTeacherPreviewName={setTeacherPreviewName}
              tForm={tForm}
              fillTeacherFormByName={fillTeacherFormByName}
              setTForm={setTForm}
              saveTeacher={saveTeacher}
              groups={groups}
              paginate={paginate}
              renderPager={renderPager}
              deleteTeacher={deleteTeacher}
              setMessage={setMessage}
              onDeleteTeacherByName={deleteTeacherByName}
              onBulkSetTeacherStatus={bulkSetTeacherStatus}
              onBulkDeleteTeachers={bulkDeleteTeachers}
              onAssignCurator={assignTeacherCurator}
              onImportTeachersBatch={importTeachersBatch}
              onOpenDisciplineModal={(teacherName) => {
                const first = disciplineRows.find((d) => d.teacherName === teacherName) || disciplineRows[0] || {};
                const faculty = first.faculty || facultiesCatalog[0] || "";
                setTeacherDisciplineModal({
                  teacherName,
                  disciplineName: first.name || "",
                  faculty,
                  specialty: first.specialty || specialtiesCatalog[faculty]?.[0] || "",
                  course: first.course || "1 курс"
                });
              }}
              searchTerm={search}
              onClearSearch={() => setSearch("")}
            />
          )}

          {adminTab === "disciplines" && (() => {
            const courseOptions = ["1 курс", "2 курс", "3 курс", "4 курс"];
            const disciplineFilteredRows = filtered(disciplineRows, (d) => `${d.name} ${disciplineFullName(d.name)} ${d.teacherName} ${d.faculty} ${facultyFullName(d.faculty)} ${d.specialty} ${d.course}`)
              .filter((d) => !dForm.faculty || d.faculty === dForm.faculty)
              .filter((d) => !dForm.specialty || d.specialty === dForm.specialty)
              .filter((d) => !dForm.course || d.course === dForm.course)
              .filter((d) => !dForm.teacherName || d.teacherName === dForm.teacherName)
              .sort((a, b) => String(a.name || "").localeCompare(String(b.name || ""), "ru"));
            const disciplinePageRows = paginate(disciplineFilteredRows, adminDisciplinesPage);
            return (
            <section className="admin-modern-page">
              <div className="admin-page-head">
                <div>
                  <h2>Дисциплины</h2>
                </div>
                <button type="button" className="btn-main" onClick={() => { setDForm({ editTarget: "", name: "", fullName: "", teacherName: "", faculty: facultiesCatalog[0] || "", specialty: specialtiesCatalog[facultiesCatalog[0]]?.[0] || "", course: "1 курс" }); setAdminDisciplinesPage(1); }}>+ Добавить дисциплину</button>
              </div>
              <div className="admin-filter-card">
                <input placeholder="Поиск" value={search} onChange={(e) => { setSearch(e.target.value); setAdminDisciplinesPage(1); }} />
                <select value={dForm.faculty} onChange={(e) => {
                  const faculty = e.target.value;
                  setDForm((p) => ({ ...p, faculty, specialty: specialtiesCatalog[faculty]?.[0] || "" }));
                  setAdminDisciplinesPage(1);
                }}>
                  <option value="">Все факультеты</option>
                  {facultiesCatalog.map((faculty) => <option key={faculty} value={faculty} title={facultyFullName(faculty)}>{faculty}</option>)}
                </select>
                <select value={dForm.specialty} onChange={(e) => { setDForm((p) => ({ ...p, specialty: e.target.value })); setAdminDisciplinesPage(1); }}>
                  <option value="">Все специальности</option>
                  {(dForm.faculty ? specialtiesCatalog[dForm.faculty] || [] : Object.values(specialtiesCatalog).flat()).map((specialty) => <option key={specialty} value={specialty}>{specialty}</option>)}
                </select>
                <select value={dForm.course} onChange={(e) => { setDForm((p) => ({ ...p, course: e.target.value })); setAdminDisciplinesPage(1); }}>
                  <option value="">Все курсы</option>
                  {courseOptions.map((course) => <option key={course} value={course}>{course}</option>)}
                </select>
                <select value={dForm.teacherName} onChange={(e) => { setDForm((p) => ({ ...p, teacherName: e.target.value })); setAdminDisciplinesPage(1); }}>
                  <option value="">Все преподаватели</option>
                  {teachers.map((t) => <option key={t.id} value={UiUtils.fullName(t)}>{UiUtils.fullName(t)}</option>)}
                </select>
                <button type="button" className="btn-ghost" onClick={() => { setSearch(""); setDForm((p) => ({ ...p, teacherName: "", faculty: "", specialty: "", course: "" })); setAdminDisciplinesPage(1); }}>Сбросить фильтры</button>
              </div>
              <div className="admin-modern-grid">
                <section className="card admin-table-card">
                  <table className="admin-data-table admin-discipline-table">
                    <thead><tr><th>Наименование</th><th>Курс</th><th>Факультет</th><th>Специальность</th><th>Преподаватель</th><th>Действия</th></tr></thead>
                    <tbody>
                      {disciplinePageRows.map((d) => (
                          <tr key={d.id || d.name}>
                            <td><strong><ShortWithTooltip value={d.name} full={disciplineFullName(d.name)} /></strong><span className="table-sub">{disciplineFullName(d.name)}</span></td>
                            <td>{d.course || "—"}</td>
                            <td><ShortWithTooltip value={d.faculty} full={facultyFullName(d.faculty)} /></td>
                            <td>{d.specialty || "—"}</td>
                            <td>{d.teacherName || "Не назначен"}</td>
                            <td className="admin-row-actions">
                              <button type="button" className="btn-ghost" onClick={() => fillDisciplineFormByName(d.name)} title="Редактировать" aria-label={`Редактировать ${d.name}`}><ActionIcon name="edit" /></button>
                            </td>
                          </tr>
                        ))}
                      {disciplinePageRows.length === 0 ? <tr><td colSpan={6} className="sub">Дисциплины не найдены.</td></tr> : null}
                    </tbody>
                  </table>
                  {renderPager(disciplineFilteredRows.length, adminDisciplinesPage, setAdminDisciplinesPage)}
                </section>
                <aside className="card admin-side-editor">
                  <h3>{dForm.editTarget ? "Редактировать дисциплину" : "Новая дисциплина"}</h3>
                  <label>Факультет<select value={dForm.faculty} onChange={(e) => {
                    const faculty = e.target.value;
                    setDForm((p) => ({ ...p, faculty, specialty: specialtiesCatalog[faculty]?.[0] || "" }));
                  }}><option value="">Факультет</option>{facultiesCatalog.map((faculty) => <option key={faculty} value={faculty} title={facultyFullName(faculty)}>{faculty}</option>)}</select></label>
                  <label>Специальность<select value={dForm.specialty} onChange={(e) => setDForm((p) => ({ ...p, specialty: e.target.value }))}><option value="">Специальность</option>{(specialtiesCatalog[dForm.faculty] || []).map((specialty) => <option key={specialty} value={specialty}>{specialty}</option>)}</select></label>
                  <label>Курс<select value={dForm.course} onChange={(e) => setDForm((p) => ({ ...p, course: e.target.value }))}><option value="">Курс</option>{courseOptions.map((course) => <option key={course} value={course}>{course}</option>)}</select></label>
                  <label>Сокращение<input value={dForm.name} onChange={(e) => setDForm((p) => ({ ...p, name: e.target.value }))} placeholder="Например, ОАиП" /></label>
                  <label>Полное название<input value={dForm.fullName} onChange={(e) => setDForm((p) => ({ ...p, fullName: e.target.value }))} placeholder="Например, Основы алгоритмизации и программирования" /></label>
                  <label>Преподаватель<select value={dForm.teacherName} onChange={(e) => setDForm((p) => ({ ...p, teacherName: e.target.value }))}><option value="">Преподаватель</option>{teachers.map((t) => <option key={t.id} value={UiUtils.fullName(t)}>{UiUtils.fullName(t)}</option>)}</select></label>
                  <div className="row">
                    <button type="button" className="btn-main" onClick={saveDiscipline}>Сохранить</button>
                    <button type="button" className="btn-danger" onClick={deleteDiscipline}>Удалить</button>
                  </div>
                </aside>
              </div>
            </section>
            );
          })()}

          {adminTab === "faculties" && (() => {
            const activeStats = facultyStats.find((item) => item.faculty === activeAdminFaculty) || { specialtiesCount: 0, groupsCount: 0, studentsCount: 0 };
            const shownFaculties = facultyStats
              .filter((item) => `${item.faculty} ${facultyFullName(item.faculty)}`.toLowerCase().includes(facultySearch.toLowerCase()))
              .sort((a, b) => a.faculty.localeCompare(b.faculty, "ru"));
            return (
            <section className="admin-modern-page admin-faculty-page">
              <div className="admin-page-head">
                <div>
                  <h2>Факультеты и специальности</h2>
                </div>
              </div>
              <div className="admin-faculty-hierarchy">
                <aside className="card faculty-list-panel">
                  <div className="admin-breadcrumbs">Факультеты</div>
                  <input placeholder="Поиск факультета..." value={facultySearch} onChange={(e) => setFacultySearch(e.target.value)} />
                  <div className="faculty-tree-list">
                    {shownFaculties.map((row) => (
                      <button key={row.faculty} type="button" className={`faculty-tree-item ${activeAdminFaculty === row.faculty ? "is-active" : ""}`} onClick={() => setSelectedAdminFaculty(row.faculty)}>
                        <span className="faculty-folder"><ActionIcon name="faculty" /></span>
                        <strong><ShortWithTooltip value={row.faculty} full={facultyFullName(row.faculty)} /></strong>
                        <span className="faculty-full-name">{facultyFullName(row.faculty)}</span>
                        <small>{row.specialtiesCount} спец. · {row.studentsCount} студ.</small>
                      </button>
                    ))}
                  </div>
                  <div className="faculty-add-row">
                    <input value={facultyDraft} onChange={(e) => setFacultyDraft(e.target.value)} placeholder="Сокращение, например ФКСиС" />
                    <input value={facultyFullDraft} onChange={(e) => setFacultyFullDraft(e.target.value)} placeholder="Полное название факультета" />
                    <button type="button" className="btn-main" onClick={() => { addFacultyToCatalog(facultyDraft, facultyFullDraft); setSelectedAdminFaculty(facultyDraft.trim()); setFacultyDraft(""); setFacultyFullDraft(""); }}>+ Добавить факультет</button>
                  </div>
                </aside>
                <section className="card faculty-detail-panel">
                  <div className="admin-breadcrumbs">Факультеты &gt; {activeAdminFaculty || "Факультет"}</div>
                  <div className="faculty-detail-head">
                    <div>
                      {editingFaculty === activeAdminFaculty ? (
                        <div className="inline-edit-row">
                          <input value={facultyDraft} onChange={(e) => setFacultyDraft(e.target.value)} placeholder="Сокращение" />
                          <input value={facultyFullDraft} onChange={(e) => setFacultyFullDraft(e.target.value)} placeholder="Полное название" />
                          <button type="button" className="btn-main" onClick={() => renameFaculty(activeAdminFaculty, facultyDraft, facultyFullDraft)}>Сохранить</button>
                          <button type="button" className="btn-ghost" onClick={() => setEditingFaculty(null)}>Отмена</button>
                        </div>
                      ) : (
                        <h3><ShortWithTooltip value={activeAdminFaculty || "Выберите факультет"} full={facultyFullName(activeAdminFaculty)} /></h3>
                      )}
                      {activeAdminFaculty ? <p className="sub">{facultyFullName(activeAdminFaculty)}</p> : null}
                      <p className="sub">{activeStats.specialtiesCount} специальности · {activeStats.groupsCount} групп · {activeStats.studentsCount} студентов</p>
                    </div>
                    {activeAdminFaculty && (
                      <div className="admin-row-actions">
                        <button type="button" className="btn-ghost" onClick={() => { setEditingFaculty(activeAdminFaculty); setFacultyDraft(activeAdminFaculty); setFacultyFullDraft(facultyFullName(activeAdminFaculty)); }} title="Редактировать"><ActionIcon name="edit" /></button>
                        <button type="button" className="btn-danger" onClick={() => setPendingFacultyDelete(activeAdminFaculty)} title="Удалить"><ActionIcon name="trash" /></button>
                      </div>
                    )}
                  </div>
                  <div className="admin-kpi-grid admin-kpi-grid--disciplines">
                    <article className="admin-kpi-card"><span>Специальностей</span><strong>{activeStats.specialtiesCount}</strong><em>в факультете</em></article>
                    <article className="admin-kpi-card"><span>Групп</span><strong>{activeStats.groupsCount}</strong><em>всего</em></article>
                    <article className="admin-kpi-card"><span>Студентов</span><strong>{activeStats.studentsCount}</strong><em>по группам</em></article>
                  </div>
                  <h4>Специальности</h4>
                  <div className="specialty-list">
                    {(specialtiesCatalog[activeAdminFaculty] || []).map((specialty) => {
                      const stats = specialtyStatsForFaculty(activeAdminFaculty, specialty);
                      const specialtyFull = specialtyFullName(activeAdminFaculty, specialty);
                      return (
                        <div key={specialty} className="specialty-row">
                          {editingSpecialty === specialty ? (
                            <div className="inline-edit-row">
                              <input value={specialtyDraft} onChange={(e) => setSpecialtyDraft(e.target.value)} />
                              <input value={specialtyFullDraft} onChange={(e) => setSpecialtyFullDraft(e.target.value)} placeholder="Полное название специальности" />
                              <button type="button" className="btn-main" onClick={() => renameSpecialty(activeAdminFaculty, specialty, specialtyDraft, specialtyFullDraft)}>Сохранить</button>
                              <button type="button" className="btn-ghost" onClick={() => setEditingSpecialty(null)}>Отмена</button>
                            </div>
                          ) : (
                            <>
                              <div>
                                <strong><ShortWithTooltip value={specialty} full={specialtyFull} /></strong>
                                <em className="specialty-full-name">{specialtyFull}</em>
                                <span>{stats.groupsCount} групп · {stats.studentsCount} студентов</span>
                              </div>
                              <div className="admin-row-actions">
                                <button type="button" className="btn-ghost" onClick={() => { setGForm({ editTarget: "", number: "", course: "1 курс", faculty: activeAdminFaculty, specialty, curator: "" }); setAdminTab("groups"); }}>Создать группу</button>
                                <button type="button" className="btn-ghost" onClick={() => setAdminTab("students")}>К студентам</button>
                                <button type="button" className="btn-ghost" onClick={() => { setEditingSpecialty(specialty); setSpecialtyDraft(specialty); setSpecialtyFullDraft(specialtyFull); }} title="Редактировать"><ActionIcon name="edit" /></button>
                                <button type="button" className="btn-danger" onClick={() => setPendingSpecialtyDelete({ faculty: activeAdminFaculty, specialty })} title="Удалить"><ActionIcon name="trash" /></button>
                              </div>
                            </>
                          )}
                        </div>
                      );
                    })}
                  </div>
                  <div className="faculty-add-row">
                    <input value={specialtyDraft} onChange={(e) => setSpecialtyDraft(e.target.value)} placeholder="Новая специальность" />
                    <input value={specialtyFullDraft} onChange={(e) => setSpecialtyFullDraft(e.target.value)} placeholder="Полное название специальности" />
                    <button type="button" className="btn-main" onClick={() => { addSpecialtyToCatalog(activeAdminFaculty, specialtyDraft, specialtyFullDraft); setSpecialtyDraft(""); setSpecialtyFullDraft(""); }}>+ Добавить специальность</button>
                  </div>
                </section>
              </div>
              {pendingFacultyDelete && (
                <div className="note-modal-backdrop" role="dialog" aria-modal="true">
                  <div className="note-modal danger-confirm-modal">
                    <h3>Удалить факультет {pendingFacultyDelete}?</h3>
                    <p>Перед удалением проверьте связанные данные.</p>
                    <div className="danger-summary">
                      <strong>Будут затронуты:</strong>
                      <span>{facultyStats.find((item) => item.faculty === pendingFacultyDelete)?.groupsCount || 0} групп</span>
                      <span>{facultyStats.find((item) => item.faculty === pendingFacultyDelete)?.studentsCount || 0} студентов</span>
                    </div>
                    <div className="note-modal-actions">
                      <button type="button" className="btn-danger" onClick={() => deleteFacultyFromCatalog(pendingFacultyDelete)}>Удалить</button>
                      <button type="button" className="btn-ghost" onClick={() => setPendingFacultyDelete(null)}>Отмена</button>
                    </div>
                  </div>
                </div>
              )}
              {pendingSpecialtyDelete && (
                <div className="note-modal-backdrop" role="dialog" aria-modal="true">
                  <div className="note-modal danger-confirm-modal">
                    <h3>Удалить специальность {pendingSpecialtyDelete.specialty}?</h3>
                    <div className="danger-summary">
                      <strong>Будут затронуты:</strong>
                      <span>{specialtyStatsForFaculty(pendingSpecialtyDelete.faculty, pendingSpecialtyDelete.specialty).groupsCount} групп</span>
                      <span>{specialtyStatsForFaculty(pendingSpecialtyDelete.faculty, pendingSpecialtyDelete.specialty).studentsCount} студентов</span>
                    </div>
                    <div className="note-modal-actions">
                      <button type="button" className="btn-danger" onClick={() => deleteSpecialtyFromCatalog(pendingSpecialtyDelete.faculty, pendingSpecialtyDelete.specialty)}>Удалить</button>
                      <button type="button" className="btn-ghost" onClick={() => setPendingSpecialtyDelete(null)}>Отмена</button>
                    </div>
                  </div>
                </div>
              )}
            </section>
            );
          })()}

          {adminTab === "groups" && (
            <AdminGroupsSection
              groupRows={groupRows}
              groups={groups}
              filtered={filtered}
              gForm={gForm}
              fillGroupFormByNumber={fillGroupFormByNumber}
              setGForm={setGForm}
              saveGroup={saveGroup}
              deleteGroup={deleteGroup}
              facultiesCatalog={facultiesCatalog}
              specialtiesCatalog={specialtiesCatalog}
              teachers={teachers}
              students={students}
              UiUtils={UiUtils}
              studentMetaOf={studentMetaOf}
              setAdminTab={setAdminTab}
              avgMap={avgMap}
              groupMeta={groupMeta}
              avatars={avatars}
              setStudentPreviewName={setStudentPreviewName}
              onAssignStarosta={assignStarosta}
              onBulkMoveStudents={bulkMoveStudentsToGroup}
              onBulkSetStudentStatus={bulkSetStudentStatus}
              onBulkDeleteStudents={bulkDeleteStudents}
              paginate={paginate}
              renderPager={renderPager}
            />
          )}

          {adminTab === "grades" && (
            <AdminJournalSection
              adminJournalFilters={adminJournalFilters}
              setAdminJournalFilters={setAdminJournalFilters}
              groups={groups}
              disciplines={disciplines}
              teachers={teachers}
              students={students}
              UiUtils={UiUtils}
              adminJournalInsights={adminJournalInsights}
              grades={grades}
              absences={absences}
              absenceForm={absenceForm}
              setAbsenceForm={setAbsenceForm}
              journalPage={adminJournalPage}
              setJournalPage={setAdminJournalPage}
              paginate={paginate}
              renderPager={renderPager}
              saveAdminJournalGrade={saveAdminJournalGrade}
              saveAdminJournalAbsence={saveAdminJournalAbsence}
              gradeLessonMeta={gradeLessonMeta}
              ACADEMIC_HOURS_PER_ABSENCE={ACADEMIC_HOURS_PER_ABSENCE}
            />
          )}

          {adminTab === "schedule" && (
            <section className="admin-modern-page admin-schedule-page">
              <div className="admin-page-head">
                <div>
                  <h2>Расписание</h2>
                </div>
              </div>
              <div className="admin-filter-card admin-schedule-toolbar">
                <input placeholder="Поиск расписания" value={adminScheduleFilters.query} onChange={(e) => setAdminScheduleFilters((p) => ({ ...p, query: e.target.value }))} />
                <select value={adminScheduleFilters.group} onChange={(e) => {
                  const group = e.target.value;
                  setAdminScheduleFilters((p) => ({ ...p, group }));
                  setSelectedAdminLessonId(null);
                  setAdminLessonForm((p) => ({ ...p, group }));
                }}><option value="">Выберите группу</option>{groups.map((g) => <option key={g.id} value={g.number}>{g.number}</option>)}</select>
              </div>
              <div className="admin-schedule-layout">
                <section className="card admin-schedule-board">
                  {adminScheduleFilters.group ? (
                    <div className="admin-calendar-grid admin-calendar-grid--six">
                      <div className="admin-calendar-corner" />
                      {scheduleDays.map((day) => <div key={day} className="admin-calendar-head">{dayShortName(day)}<span>{day} · {lessonDateByDay[day] || ""}</span></div>)}
                      {lessonSlots.map((slot) => (
                        <Fragment key={slot.number}>
                          <div className="admin-calendar-time">{slot.start}<span>{slot.end}</span></div>
                          {scheduleDays.map((day) => {
                            const lessons = filteredAdminScheduleRows.filter((lesson) => lesson.day === day && Number(lesson.slot) === Number(slot.number));
                            return (
                              <div key={`${day}-${slot.number}`} className="admin-calendar-cell">
                                {lessons.map((lesson, idx) => (
                                  <button
                                    key={lesson.calendarId}
                                    type="button"
                                    className={`admin-lesson-card color-${idx % 5} ${lesson.conflict ? "has-conflict" : ""} ${lesson.changed ? "is-changed" : ""} ${lesson.cancelled ? "is-cancelled" : ""}`}
                                    onClick={() => fillAdminLessonForm(lesson)}
                                  >
                                    <strong>{lesson.discipline}</strong>
                                    <span>{lesson.group} · {lesson.typeMeta?.label || lesson.lessonType}</span>
                                    <small>{lesson.teacher}</small>
                                    <small>ауд. {lesson.room}</small>
                                  </button>
                                ))}
                                {lessons.length === 0 && <button type="button" className="calendar-add" onClick={() => startAdminLessonAt(day, slot.number)}>+</button>}
                              </div>
                            );
                          })}
                        </Fragment>
                      ))}
                    </div>
                  ) : (
                    <p className="edu-widget-empty">Выберите группу для просмотра расписания.</p>
                  )}
                </section>
                <aside className="admin-schedule-side">
                  <section className="card admin-side-editor">
                    <h3>{adminLessonForm.editId || adminLessonForm.replacesKey ? "Изменить пару" : "Добавить пару"}</h3>
                    <label>День<select value={adminLessonForm.day} onChange={(e) => setAdminLessonForm((p) => ({ ...p, day: e.target.value }))}>{scheduleDays.map((d) => <option key={d} value={d}>{d}</option>)}</select></label>
                    <label>Пара<select value={adminLessonForm.slot} onChange={(e) => setAdminLessonForm((p) => ({ ...p, slot: Number(e.target.value) }))}>{lessonSlots.map((s) => <option key={s.number} value={s.number}>{`${s.number} пара (${s.start})`}</option>)}</select></label>
                    <label>Тип занятия<select value={adminLessonForm.lessonType} onChange={(e) => setAdminLessonForm((p) => ({ ...p, lessonType: e.target.value }))}>{LESSON_TYPES.map((type) => <option key={type.code} value={type.code}>{type.title}</option>)}</select></label>
                    <label>Предмет<select value={adminLessonForm.discipline} onChange={(e) => setAdminLessonForm((p) => ({ ...p, discipline: e.target.value }))}><option value="">Дисциплина</option>{disciplines.map((d) => <option key={d.id} value={d.name}>{d.name}</option>)}</select></label>
                    <label>Группа<select value={adminLessonForm.group} onChange={(e) => setAdminLessonForm((p) => ({ ...p, group: e.target.value }))}><option value="">Группа</option>{groups.map((g) => <option key={g.id} value={g.number}>{g.number}</option>)}</select></label>
                    <label>Преподаватель<select value={adminLessonForm.teacher} onChange={(e) => setAdminLessonForm((p) => ({ ...p, teacher: e.target.value }))}><option value="">Преподаватель</option>{teachers.map((t) => <option key={t.id} value={UiUtils.fullName(t)}>{UiUtils.fullName(t)}</option>)}</select></label>
                    <label>Аудитория<input value={adminLessonForm.room} onChange={(e) => setAdminLessonForm((p) => ({ ...p, room: e.target.value }))} /></label>
                    <button type="button" className="btn-main" onClick={addAdminLesson}>Сохранить пару</button>
                    {(adminLessonForm.editId || adminLessonForm.replacesKey) && <button type="button" className="btn-danger" onClick={removeScheduleLesson}>Удалить пару</button>}
                    {selectedAdminLesson?.conflict && <div className="conflict-box">Конфликт расписания. Измените аудиторию, преподавателя или время.</div>}
                  </section>
                </aside>
              </div>
            </section>
          )}

          {adminTab === "access" && (() => {
            const accessRows = [...userAccounts].sort((a, b) => String(a.fullName || "").localeCompare(String(b.fullName || ""), "ru"));
            const accessPageRows = paginate(accessRows, adminAccessPage);
            return (
          <section className="card">
            <h3>Права преподавателей</h3>
            <p className="sub">Назначайте преподавателей из зарегистрированных пользователей. После назначения доступ к оценкам, расписанию и пропускам выдается автоматически.</p>
            <table>
              <thead><tr><th>ФИО</th><th>Email</th><th>Статус</th><th>Действия</th></tr></thead>
              <tbody>
                {accessPageRows.map((account, idx) => {
                  const hasAccess = teacherAccessEmails.some((mail) => normalizeEmail(mail) === normalizeEmail(account.email));
                  const isTeacher = teachers.some((t) => UiUtils.fullName(t).toLowerCase() === account.fullName.toLowerCase());
                  return (
                    <tr key={`${account.email}-${idx}`}>
                      <td>{account.fullName}</td>
                      <td>{account.email}</td>
                      <td>{hasAccess ? "Преподаватель (доступ выдан)" : (isTeacher ? "В базе преподавателей" : "Студент/пользователь")}</td>
                      <td>
                        {!hasAccess && <button type="button" className="btn-main" onClick={() => grantTeacherAccess(account.email)}>Назначить преподавателем</button>}
                        {hasAccess && <button type="button" className="btn-danger" onClick={() => revokeTeacherAccess(account.email)}>Снять роль преподавателя</button>}
                      </td>
                    </tr>
                  );
                })}
                {accessPageRows.length === 0 ? <tr><td colSpan={4} className="sub">Пользователи не найдены.</td></tr> : null}
              </tbody>
            </table>
            {renderPager(accessRows.length, adminAccessPage, setAdminAccessPage)}
          </section>
            );
          })()}
          {renderStudentPeekModal()}
          {teacherPreviewName && previewTeacherProfile && (
            <div className="note-modal-backdrop" onClick={() => setTeacherPreviewName(null)}>
              <div className="teacher-profile-modal" onClick={(e) => e.stopPropagation()}>
                <h3>{teacherPreviewName}</h3>
                <div className="teacher-preview-avatar-wrap">
                  <img className="teacher-preview-avatar" src={previewTeacherProfile.avatar || ""} alt="" />
                </div>
                <div className="teacher-profile-grid">
                  <div>
                    <h4>Ученая степень</h4>
                    <p>{previewTeacherProfile.degree || "Не указано"}</p>
                    <h4>Должность, место работы</h4>
                    <p>{previewTeacherProfile.position || "Не указано"}</p>
                    <p>{previewTeacherProfile.department || "Не указано"}</p>
                  </div>
                  <div>
                    <h4>Контакты</h4>
                    <p>Почта: {previewTeacherProfile.email || "Не указана"}</p>
                    <p>Рабочий телефон: {previewTeacherProfile.workPhone ? formatBelarusPhone(previewTeacherProfile.workPhone) : "Не указан"}</p>
                    <p>{previewTeacherProfile.office || "Кабинет не указан"}</p>
                  </div>
                </div>
                <h4>Читаемые курсы</h4>
                <p>{previewTeacherProfile.courses || "Не указано"}</p>
              </div>
            </div>
          )}
          </div>
        </main>
      </div>
    </div>
  );
}

export default App;

