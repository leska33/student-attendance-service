import { useMemo, useState } from "react";

const EMPTY_IMPORT_ROW = { lastName: "", firstName: "", middleName: "", fullName: "" };
const EMPTY_TEACHER_IMPORT_ROW = { lastName: "", firstName: "", middleName: "" };

function AdminActionIcon({ name }) {
  const paths = {
    edit: "M4 20h4l10.5-10.5a2.1 2.1 0 0 0-3-3L5 17v3Zm12-14 3 3",
    trash: "M4 7h16M9 7V5h6v2M7 7l1 13h8l1-13M10 11v5M14 11v5",
    eye: "M2.5 12s3.5-6 9.5-6 9.5 6 9.5 6-3.5 6-9.5 6-9.5-6-9.5-6Zm9.5 3a3 3 0 1 0 0-6 3 3 0 0 0 0 6Z"
  };
  return (
    <svg className="menu-svg-icon" viewBox="0 0 24 24" fill="none" aria-hidden="true">
      <path d={paths[name] || paths.edit} stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" />
    </svg>
  );
}

const parseStudentLine = (line) => {
  const trimmed = String(line || "").trim();
  if (!trimmed) return null;
  const parts = trimmed.split(",").map((x) => x.trim());
  if (parts.length >= 2) {
    const name = splitName(parts[0]);
    return {
      ...EMPTY_IMPORT_ROW,
      fullName: parts[0],
      ...name
    };
  }
  return { ...EMPTY_IMPORT_ROW, fullName: trimmed, ...splitName(trimmed) };
};

const splitName = (fullName) => {
  const parts = String(fullName || "").trim().split(/\s+/).filter(Boolean);
  return { lastName: parts[0] || "", firstName: parts[1] || "", middleName: parts.slice(2).join(" ") || "" };
};

const parseTeacherLine = (line) => {
  const trimmed = String(line || "").trim();
  if (!trimmed) return null;
  const parts = trimmed.split(",").map((x) => x.trim());
  const name = splitName(parts[0] || "");
  return {
    ...EMPTY_TEACHER_IMPORT_ROW,
    ...name
  };
};

export function AdminStudentsSection(props) {
  const {
    adminGroupedStudents,
    adminGroupedPage,
    setAdminGroupedPage,
    paginate,
    UiUtils,
    studentMetaOf,
    avatars,
    setStudentPreviewName,
    renderPager,
    avgMap,
    setMessage,
    groups,
    STUDENT_STATUSES,
    onImportStudentsBatch,
    searchTerm = "",
    stForm,
    fillStudentFormByName,
    setStForm,
    saveStudent,
    onBulkMoveStudents,
    onBulkSetStudentStatus,
    onBulkDeleteStudents,
    onClearSearch
  } = props;

  const [selectedIds, setSelectedIds] = useState([]);
  const [massAction, setMassAction] = useState("");
  const [bulkGroup, setBulkGroup] = useState("");
  const [bulkStatus, setBulkStatus] = useState("Активен");
  const [showMassImport, setShowMassImport] = useState(false);
  const [importRows, setImportRows] = useState([{ ...EMPTY_IMPORT_ROW }, { ...EMPTY_IMPORT_ROW }, { ...EMPTY_IMPORT_ROW }]);
  const [isLoading, setIsLoading] = useState(false);
  const [transferGroup, setTransferGroup] = useState(groups?.[0]?.number || "");
  const [dragStudentName, setDragStudentName] = useState("");
  const [isDropZoneActive, setIsDropZoneActive] = useState(false);
  const [showStudentNameForm, setShowStudentNameForm] = useState(false);

  const normalizedSearch = searchTerm.toLowerCase();
  const visibleStudents = useMemo(() => (
    (adminGroupedStudents || [])
      .filter((s) => `${UiUtils.fullName(s)} ${s.groupNumber || ""}`.toLowerCase().includes(normalizedSearch))
      .sort((a, b) => UiUtils.fullName(a).localeCompare(UiUtils.fullName(b), "ru"))
  ), [adminGroupedStudents, UiUtils, normalizedSearch]);
  const rows = useMemo(() => paginate(visibleStudents, adminGroupedPage), [paginate, visibleStudents, adminGroupedPage]);
  const allSelected = rows.length > 0 && rows.every((x) => selectedIds.includes(x.id));
  const selectedNames = visibleStudents.filter((x) => selectedIds.includes(x.id)).map((x) => UiUtils.fullName(x));
  const studentsWithoutGroup = [...(adminGroupedStudents || []).filter((s) => !s.groupNumber)].sort((a, b) => UiUtils.fullName(a).localeCompare(UiUtils.fullName(b), "ru"));
  const studentsInTargetGroup = [...(adminGroupedStudents || []).filter((s) => String(s.groupNumber) === String(transferGroup))].sort((a, b) => UiUtils.fullName(a).localeCompare(UiUtils.fullName(b), "ru"));

  const toggleAll = () => {
    if (allSelected) setSelectedIds([]);
    else setSelectedIds(rows.map((x) => x.id));
  };
  const toggleOne = (id) => setSelectedIds((prev) => (prev.includes(id) ? prev.filter((x) => x !== id) : [...prev, id]));

  const statusClass = (status) => {
    const s = String(status || "").toLowerCase();
    if (s.includes("отчис")) return "student-status-pill student-status-pill--dismissed";
    if (s.includes("больнич")) return "student-status-pill student-status-pill--sick";
    if (s.includes("академ")) return "student-status-pill student-status-pill--vacation";
    return "student-status-pill student-status-pill--active";
  };

  const runMassAction = async () => {
    if (selectedNames.length === 0) return setMessage("Выберите студентов.");
    if (massAction === "delete") {
      await onBulkDeleteStudents(selectedNames);
    } else if (massAction === "rename") {
      if (selectedNames.length !== 1) return setMessage("Выберите одного студента для изменения ФИО.");
      fillStudentFormByName(selectedNames[0]);
      setShowStudentNameForm(true);
      return;
    } else {
      setMessage("Выберите действие.");
      return;
    }
    setSelectedIds([]);
  };
  const submitStudentNameForm = async (e) => {
    e.preventDefault();
    if (!String(stForm.lastName || "").trim() || !String(stForm.firstName || "").trim()) {
      setMessage("Заполните фамилию и имя студента.");
      return;
    }
    await saveStudent();
    setShowStudentNameForm(false);
    setSelectedIds([]);
    setAdminGroupedPage(1);
    onClearSearch?.();
  };

  const updateImportCell = (idx, key, value) => {
    setImportRows((prev) => prev.map((row, i) => (i === idx ? { ...row, [key]: value } : row)));
  };
  const removeImportRow = (idx) => {
    setImportRows((prev) => prev.filter((_, i) => i !== idx));
  };

  const pasteIntoMassImport = (e) => {
    const text = e.clipboardData?.getData("text") || "";
    if (!text.trim()) return;
    e.preventDefault();
    const lines = text.split(/\r?\n/).map((x) => x.trim()).filter(Boolean);
    if (lines.length === 0) return;
    const parsed = lines.map((line) => parseStudentLine(line)).filter(Boolean);
    if (parsed.length > 0) setImportRows(parsed);
  };

  const handleDropFile = async (e) => {
    e.preventDefault();
    const file = e.dataTransfer?.files?.[0];
    if (!file) return;
    try {
      const ext = file.name.split(".").pop()?.toLowerCase();
      let rowsFromFile = [];
      if (ext === "csv") {
        const text = await file.text();
        rowsFromFile = text.split(/\r?\n/).map((line) => parseStudentLine(line)).filter(Boolean);
      } else if (ext === "xlsx") {
        const xlsx = await import("xlsx");
        const buffer = await file.arrayBuffer();
        const wb = xlsx.read(buffer, { type: "array" });
        const sheet = wb.Sheets[wb.SheetNames[0]];
        const data = xlsx.utils.sheet_to_json(sheet, { header: 1 });
        rowsFromFile = (data || [])
          .map((row) => parseStudentLine((row || []).join(",")))
          .filter(Boolean);
      } else {
        setMessage("Поддерживаются только .csv и .xlsx");
        return;
      }
      if (rowsFromFile.length === 0) return setMessage("Файл пуст или не распознан.");
      setImportRows(rowsFromFile);
      setShowMassImport(true);
    } catch {
      setMessage("Не удалось обработать файл.");
    }
  };

  const runMassImport = async () => {
    const valid = importRows
      .map((row) => ({
        ...row,
        fullName: `${row.lastName || ""} ${row.firstName || ""} ${row.middleName || ""}`.trim()
      }))
      .filter((x) => String(x.lastName || "").trim() && String(x.firstName || "").trim());
    if (valid.length === 0) return setMessage("Заполните хотя бы фамилию и имя.");
    await onImportStudentsBatch?.(valid);
  };

  const handleRefresh = async () => {
    setIsLoading(true);
    window.setTimeout(() => setIsLoading(false), 650);
  };

  const moveSingleToTransferGroup = async (name) => {
    if (!transferGroup) return setMessage("Выберите группу справа.");
    await onBulkMoveStudents([name], transferGroup);
  };
  const onStudentDragStart = (name) => {
    setDragStudentName(name);
  };
  const onTransferDrop = async () => {
    if (!dragStudentName) return;
    await moveSingleToTransferGroup(dragStudentName);
    setDragStudentName("");
    setIsDropZoneActive(false);
  };

  return (
    <section className="card admin-section-shell">
      <h3>Управление студентами</h3>
      <div className="row">
        <button type="button" className="btn-main" onClick={() => setShowMassImport((v) => !v)}>Массовое добавление</button>
        <button type="button" className="btn-ghost" onClick={handleRefresh}>Обновить данные</button>
      </div>
      {showMassImport && (
        <div className="admin-mass-import">
          <div className="admin-dropzone" onDrop={handleDropFile} onDragOver={(e) => e.preventDefault()}>
            Перетащите .csv или .xlsx сюда
          </div>
          <div className="admin-import-grid-wrap">
            <table className="admin-data-table admin-import-grid" onPaste={pasteIntoMassImport}>
              <thead>
                <tr>
                  <th>Фамилия</th><th>Имя</th><th>Отчество</th><th />
                </tr>
              </thead>
              <tbody>
                {importRows.map((row, i) => (
                  <tr key={`import-row-${i}`}>
                    <td><input value={row.lastName || ""} onChange={(e) => updateImportCell(i, "lastName", e.target.value)} /></td>
                    <td><input value={row.firstName || ""} onChange={(e) => updateImportCell(i, "firstName", e.target.value)} /></td>
                    <td><input value={row.middleName || ""} onChange={(e) => updateImportCell(i, "middleName", e.target.value)} /></td>
                    <td>
                      <button
                        type="button"
                        className="btn-danger admin-import-remove-btn"
                        onClick={() => removeImportRow(i)}
                        title="Удалить строку"
                        aria-label="Удалить строку"
                      >
                        <AdminActionIcon name="trash" />
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          <div className="row">
            <button type="button" className="btn-main" onClick={runMassImport}>Сохранить</button>
            <button type="button" className="btn-ghost" onClick={() => setImportRows((prev) => [...prev, { ...EMPTY_IMPORT_ROW }])}>+ Строка</button>
          </div>
        </div>
      )}
      <div className="table-select-all">
        <input className="row-checkbox" type="checkbox" checked={allSelected} onChange={toggleAll} />
        <span>Выбрать все</span>
      </div>
      <div className="admin-mass-bar admin-mass-bar--top">
        <span>Выбрано: {selectedNames.length}</span>
        <select value={massAction} onChange={(e) => setMassAction(e.target.value)}>
          <option value="">Действие</option>
          <option value="rename">Изменить ФИО</option>
          <option value="delete">Удалить</option>
        </select>
        <button className="btn-main" type="button" onClick={runMassAction}>Применить</button>
      </div>
      {isLoading ? (
        <div className="admin-skeleton-list">
          <div className="admin-skeleton-row" />
          <div className="admin-skeleton-row" />
          <div className="admin-skeleton-row" />
        </div>
      ) : (
      <div className="admin-students-table-wrap">
      <table className="admin-data-table">
        <thead>
          <tr>
            <th className="col-check" />
            <th className="col-photo">Фото</th>
            <th>ФИО</th>
            <th>Группа</th>
            <th>Статус</th>
            <th>Средний балл</th>
          </tr>
        </thead>
        <tbody>
          {rows.map((s) => {
            const name = UiUtils.fullName(s);
            const meta = studentMetaOf(name);
            return (
              <tr key={s.id}>
                <td className="col-check"><input className="row-checkbox" type="checkbox" checked={selectedIds.includes(s.id)} onChange={() => toggleOne(s.id)} /></td>
                <td className="col-photo"><img className="avatar" src={avatars[s.id] || ""} alt="" /></td>
                <td><button className="profile-link-btn" onClick={() => setStudentPreviewName(name)}>{name}</button></td>
                <td><span className="pill">{s.groupNumber || "нет"}</span></td>
                <td><span className={`pill ${statusClass(meta.studentStatus || "Активен")}`}>{meta.studentStatus || "Активен"}</span></td>
                <td>{avgMap[name] || "0.0"}</td>
              </tr>
            );
          })}
        </tbody>
      </table>
      </div>
      )}
      {renderPager(visibleStudents.length, adminGroupedPage, setAdminGroupedPage)}
      {showStudentNameForm && (
        <div className="note-modal-backdrop" role="dialog" aria-modal="true">
          <form className="note-modal admin-name-form-modal" onSubmit={submitStudentNameForm}>
            <h3>Изменить ФИО студента</h3>
            <div className="admin-name-form-grid">
              <label>Фамилия<input value={stForm.lastName} onChange={(e) => setStForm((p) => ({ ...p, lastName: e.target.value }))} autoFocus /></label>
              <label>Имя<input value={stForm.firstName} onChange={(e) => setStForm((p) => ({ ...p, firstName: e.target.value }))} /></label>
              <label>Отчество<input value={stForm.middleName} onChange={(e) => setStForm((p) => ({ ...p, middleName: e.target.value }))} /></label>
            </div>
            <div className="note-modal-actions">
              <button className="btn-main" type="submit">Сохранить</button>
              <button className="btn-ghost" type="button" onClick={() => setShowStudentNameForm(false)}>Отмена</button>
            </div>
          </form>
        </div>
      )}
      <section className="admin-transfer-list">
        <div className="admin-transfer-col">
          <h4>Студенты без группы</h4>
          {studentsWithoutGroup.map((s) => {
            const fullName = UiUtils.fullName(s);
            return (
              <button
                key={s.id}
                type="button"
                className="admin-transfer-item"
                draggable
                onDragStart={() => onStudentDragStart(fullName)}
                onClick={() => moveSingleToTransferGroup(fullName)}
                title="Перетащите в правую колонку"
              >
                {fullName}
              </button>
            );
          })}
          {studentsWithoutGroup.length === 0 && <p className="sub">Нет студентов без группы</p>}
        </div>
        <div
          className={`admin-transfer-col ${isDropZoneActive ? "admin-transfer-col--active" : ""}`}
          onDragOver={(e) => {
            e.preventDefault();
            setIsDropZoneActive(true);
          }}
          onDragLeave={() => setIsDropZoneActive(false)}
          onDrop={(e) => {
            e.preventDefault();
            onTransferDrop();
          }}
        >
          <h4>Состав группы {transferGroup || "—"}</h4>
          <select value={transferGroup} onChange={(e) => setTransferGroup(e.target.value)}>
            <option value="">Выберите группу</option>
            {groups.map((g) => <option key={g.id} value={g.number}>{g.number}</option>)}
          </select>
          <div className="admin-transfer-drop-hint">Перетащите сюда студента мышью</div>
          {studentsInTargetGroup.map((s) => <div key={s.id} className="admin-transfer-item admin-transfer-item--target">{UiUtils.fullName(s)}</div>)}
        </div>
      </section>
    </section>
  );
}

export function AdminTeachersSection({
  teachers,
  UiUtils,
  teacherProfileOf,
  setTeacherPreviewName,
  tForm,
  fillTeacherFormByName,
  setTForm,
  saveTeacher,
  groups = [],
  paginate,
  renderPager,
  setMessage,
  onBulkSetTeacherStatus,
  onBulkDeleteTeachers,
  onAssignCurator,
  onImportTeachersBatch,
  searchTerm = "",
  onClearSearch
}) {
  const [teacherQuery, setTeacherQuery] = useState("");
  const [teacherPage, setTeacherPage] = useState(1);
  const [selectedIds, setSelectedIds] = useState([]);
  const [massAction, setMassAction] = useState("");
  const [bulkStatus, setBulkStatus] = useState("Активен");
  const [showTeacherForm, setShowTeacherForm] = useState(false);
  const [teacherFormMode, setTeacherFormMode] = useState("create");
  const [showMassImport, setShowMassImport] = useState(false);
  const [importRows, setImportRows] = useState([{ ...EMPTY_TEACHER_IMPORT_ROW }, { ...EMPTY_TEACHER_IMPORT_ROW }, { ...EMPTY_TEACHER_IMPORT_ROW }]);
  const [curatorGroup, setCuratorGroup] = useState(groups?.[0]?.number || "");

  const rows = useMemo(() => {
    const q = `${searchTerm} ${teacherQuery}`.trim().toLowerCase();
    return [...(teachers || [])]
      .filter((t) => !q || `${UiUtils.fullName(t)} ${(t.disciplines || []).join(" ")}`.toLowerCase().includes(q))
      .sort((a, b) => UiUtils.fullName(a).localeCompare(UiUtils.fullName(b), "ru"));
  }, [teachers, UiUtils, searchTerm, teacherQuery]);
  const pageRows = useMemo(() => paginate(rows, teacherPage), [paginate, rows, teacherPage]);
  const allSelected = pageRows.length > 0 && pageRows.every((x) => selectedIds.includes(x.id));
  const selectedNames = rows.filter((x) => selectedIds.includes(x.id)).map((x) => UiUtils.fullName(x));
  const toggleAll = () => setSelectedIds(allSelected ? [] : [...new Set([...selectedIds, ...pageRows.map((x) => x.id)])]);
  const toggleOne = (id) => setSelectedIds((prev) => (prev.includes(id) ? prev.filter((x) => x !== id) : [...prev, id]));

  const statusClass = (status) => {
    const s = String(status || "").toLowerCase();
    if (s.includes("больнич")) return "teacher-status-pill teacher-status-pill--sick";
    if (s.includes("отпуск")) return "teacher-status-pill teacher-status-pill--vacation";
    return "teacher-status-pill teacher-status-pill--active";
  };

  const runMassAction = async () => {
    if (selectedNames.length === 0) return setMessage("Выберите преподавателей.");
    if (massAction === "delete") await onBulkDeleteTeachers(selectedNames);
    else if (massAction === "rename") {
      if (selectedNames.length !== 1) return setMessage("Выберите одного преподавателя для изменения ФИО.");
      fillTeacherFormByName(selectedNames[0]);
      setTeacherFormMode("edit");
      setShowTeacherForm(true);
      return;
    }
    else return setMessage("Выберите действие.");
    setSelectedIds([]);
  };
  const submitTeacherForm = async (e) => {
    e.preventDefault();
    if (!String(tForm.lastName || "").trim() || !String(tForm.firstName || "").trim()) {
      setMessage("Заполните фамилию и имя преподавателя.");
      return;
    }
    await saveTeacher();
    setShowTeacherForm(false);
    setSelectedIds([]);
    setTeacherQuery("");
    setTeacherPage(1);
    onClearSearch?.();
  };
  const updateImportCell = (idx, key, value) => setImportRows((prev) => prev.map((row, i) => (i === idx ? { ...row, [key]: value } : row)));
  const pasteIntoMassImport = (e) => {
    const text = e.clipboardData?.getData("text") || "";
    if (!text.trim()) return;
    e.preventDefault();
    const parsed = text.split(/\r?\n/).map(parseTeacherLine).filter(Boolean);
    if (parsed.length) setImportRows(parsed);
  };
  const runMassImport = async () => {
    const valid = importRows.filter((row) => String(row.lastName || "").trim() && String(row.firstName || "").trim());
    if (!valid.length) return setMessage("Заполните хотя бы фамилию и имя.");
    await onImportTeachersBatch?.(valid);
  };

  return (
    <section className="card admin-section-shell">
      <h3>Преподаватели</h3>
      <div className="row">
        <input placeholder="Поиск" value={teacherQuery} onChange={(e) => { setTeacherQuery(e.target.value); setTeacherPage(1); }} />
        <button type="button" className="btn-main" onClick={() => setShowMassImport((v) => !v)}>Массовое добавление</button>
      </div>
      {showMassImport && (
        <div className="admin-mass-import">
          <div className="admin-import-grid-wrap">
            <table className="admin-data-table admin-import-grid" onPaste={pasteIntoMassImport}>
              <thead><tr><th>Фамилия</th><th>Имя</th><th>Отчество</th><th /></tr></thead>
              <tbody>
                {importRows.map((row, i) => (
                  <tr key={`teacher-import-${i}`}>
                    <td><input value={row.lastName} onChange={(e) => updateImportCell(i, "lastName", e.target.value)} /></td>
                    <td><input value={row.firstName} onChange={(e) => updateImportCell(i, "firstName", e.target.value)} /></td>
                    <td><input value={row.middleName} onChange={(e) => updateImportCell(i, "middleName", e.target.value)} /></td>
                    <td><button type="button" className="btn-danger admin-import-remove-btn" onClick={() => setImportRows((prev) => prev.filter((_, idx) => idx !== i))}><AdminActionIcon name="trash" /></button></td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          <div className="row">
            <button type="button" className="btn-main" onClick={runMassImport}>Сохранить</button>
            <button type="button" className="btn-ghost" onClick={() => setImportRows((prev) => [...prev, { ...EMPTY_TEACHER_IMPORT_ROW }])}>+ Строка</button>
          </div>
        </div>
      )}
      <div className="table-select-all">
        <input className="row-checkbox" type="checkbox" checked={allSelected} onChange={toggleAll} />
        <span>Выбрать все</span>
      </div>
      <div className="admin-mass-bar admin-mass-bar--teachers">
        <span>Выбрано: {selectedNames.length}</span>
        <select value={massAction} onChange={(e) => setMassAction(e.target.value)}>
          <option value="">Действие</option>
          <option value="rename">Изменить ФИО</option>
          <option value="delete">Удалить</option>
        </select>
        <button className="btn-main" type="button" onClick={runMassAction}>Применить</button>
      </div>
      <table className="admin-data-table">
        <thead>
          <tr>
            <th className="col-check" />
            <th className="col-photo">Фото</th>
            <th>ФИО</th>
            <th>Дисциплины</th>
            <th>Статус</th>
          </tr>
        </thead>
        <tbody>
          {pageRows.map((t) => {
            const fullName = UiUtils.fullName(t);
            const profile = teacherProfileOf(fullName);
            return (
              <tr key={t.id}>
                <td className="col-check"><input className="row-checkbox" type="checkbox" checked={selectedIds.includes(t.id)} onChange={() => toggleOne(t.id)} /></td>
                <td className="col-photo"><img className="avatar" src={profile.avatar || ""} alt="" /></td>
                <td><button className="profile-link-btn" onClick={() => setTeacherPreviewName(fullName)}>{fullName}</button></td>
                <td>
                  <div className="teacher-chip-list">
                    {(t.disciplines || []).map((d) => <span key={`${t.id}-${d}`} className="teacher-chip">{d}</span>)}
                  </div>
                </td>
                <td><span className={`pill ${statusClass(profile.workStatus || "Активен")}`}>{profile.workStatus || "Активен"}</span></td>
              </tr>
            );
          })}
        </tbody>
      </table>
      {renderPager(rows.length, teacherPage, setTeacherPage)}
      {showTeacherForm && (
        <div className="note-modal-backdrop" role="dialog" aria-modal="true">
          <form className="note-modal admin-teacher-form-modal" onSubmit={submitTeacherForm}>
            <h3>{teacherFormMode === "edit" ? "Изменить ФИО преподавателя" : "Добавить преподавателя"}</h3>
            <div className="admin-name-form-grid">
              <label>Фамилия<input value={tForm.lastName} onChange={(e) => setTForm((p) => ({ ...p, lastName: e.target.value }))} autoFocus /></label>
              <label>Имя<input value={tForm.firstName} onChange={(e) => setTForm((p) => ({ ...p, firstName: e.target.value }))} /></label>
              <label>Отчество<input value={tForm.middleName} onChange={(e) => setTForm((p) => ({ ...p, middleName: e.target.value }))} /></label>
            </div>
            <div className="note-modal-actions">
              <button className="btn-main" type="submit">{teacherFormMode === "edit" ? "Сохранить" : "Добавить"}</button>
              <button className="btn-ghost" type="button" onClick={() => setShowTeacherForm(false)}>Отмена</button>
            </div>
          </form>
        </div>
      )}
    </section>
  );
}

export function AdminGroupsSection({
  groupRows,
  groups,
  filtered,
  gForm,
  fillGroupFormByNumber,
  setGForm,
  saveGroup,
  deleteGroup,
  facultiesCatalog,
  specialtiesCatalog,
  teachers,
  students,
  UiUtils,
  studentMetaOf,
  setAdminTab,
  onBulkMoveStudents,
  paginate,
  renderPager
}) {
  const [groupSearch, setGroupSearch] = useState("");
  const [groupPage, setGroupPage] = useState(1);
  const [deleteTarget, setDeleteTarget] = useState(null);
  const [studentQuery, setStudentQuery] = useState("");
  const [selectedStudentNames, setSelectedStudentNames] = useState([]);
  const [dragStudentName, setDragStudentName] = useState("");
  const [groupFacultyFilter, setGroupFacultyFilter] = useState("");
  const [groupSpecialtyFilter, setGroupSpecialtyFilter] = useState("");
  const rows = filtered(groupRows || groups, (g) => `${g.number} ${g.course || ""} ${g.specialty || ""} ${g.faculty || ""} ${(g.students || []).join(" ")}`)
    .filter((g) => `${g.number} ${g.course || ""} ${g.specialty || ""} ${g.faculty || ""}`.toLowerCase().includes(groupSearch.toLowerCase()))
    .filter((g) => !groupFacultyFilter || g.faculty === groupFacultyFilter)
    .filter((g) => !groupSpecialtyFilter || g.specialty === groupSpecialtyFilter)
    .sort((a, b) => String(a.number || "").localeCompare(String(b.number || ""), "ru"));
  const pageRows = paginate(rows, groupPage);
  const activeGroup = (groupRows || groups).find((g) => String(g.number) === String(gForm.editTarget)) || null;
  const activeSpecialties = specialtiesCatalog[gForm.faculty] || [];
  const studentsWithoutGroup = students.filter((s) => !s.groupNumber);
  const studentOptions = studentsWithoutGroup
    .filter((s) => UiUtils.fullName(s).toLowerCase().includes(studentQuery.toLowerCase()))
    .slice(0, 8);
  const addStudentsToGroup = async (names = selectedStudentNames) => {
    if (!gForm.editTarget || names.length === 0) return;
    await onBulkMoveStudents(names, gForm.editTarget);
    setSelectedStudentNames([]);
    setStudentQuery("");
  };
  const startNewGroup = () => {
    const faculty = facultiesCatalog[0] || "";
    setGForm({ editTarget: "", number: "", course: "1 курс", faculty, specialty: specialtiesCatalog[faculty]?.[0] || "", curator: "" });
  };
  const teacherOptions = [...(teachers || [])]
    .sort((a, b) => UiUtils.fullName(a).localeCompare(UiUtils.fullName(b), "ru"));
  const saveWithWarning = () => {
    if (activeGroup && activeGroup.specialty && activeGroup.specialty !== gForm.specialty) {
      const ok = window.confirm(`Изменить специальность группы ${activeGroup.number}?\n\nСтуденты группы (${activeGroup.studentsCount || 0}) получат новую специальность: ${gForm.specialty}.`);
      if (!ok) return;
    }
    saveGroup();
  };
  const confirmDelete = async () => {
    if (!deleteTarget) return;
    await deleteGroup(deleteTarget.number);
    setDeleteTarget(null);
  };

  return (
    <section className="admin-modern-page admin-groups-page">
      <div className="admin-page-head">
        <div>
          <h2>Группы</h2>
        </div>
        <button type="button" className="btn-main" onClick={startNewGroup}>+ Создать группу</button>
      </div>
      <div className="admin-filter-card">
        <input placeholder="Поиск" value={groupSearch} onChange={(e) => { setGroupSearch(e.target.value); setGroupPage(1); }} />
        <select value={groupFacultyFilter} onChange={(e) => {
          const faculty = e.target.value;
          setGroupFacultyFilter(faculty);
          setGroupSpecialtyFilter("");
          setGroupPage(1);
        }}>
          <option value="">Все факультеты</option>
          {facultiesCatalog.map((faculty) => <option key={faculty} value={faculty}>{faculty}</option>)}
        </select>
        <select value={groupSpecialtyFilter} onChange={(e) => { setGroupSpecialtyFilter(e.target.value); setGroupPage(1); }}>
          <option value="">Все специальности</option>
          {(groupFacultyFilter ? specialtiesCatalog[groupFacultyFilter] || [] : Object.values(specialtiesCatalog).flat()).map((specialty) => <option key={specialty} value={specialty}>{specialty}</option>)}
        </select>
        <button type="button" className="btn-ghost" onClick={() => { setGroupSearch(""); setGroupFacultyFilter(""); setGroupSpecialtyFilter(""); setGroupPage(1); }}>Сбросить</button>
      </div>
      <div className="admin-modern-grid admin-groups-grid">
        <section className="card admin-table-card">
          <table className="admin-data-table">
            <thead><tr><th>Группа</th><th>Курс</th><th>Специальность</th><th>Факультет</th><th>Студентов</th><th>Действия</th></tr></thead>
            <tbody>
              {pageRows.map((g) => (
                <tr key={g.id || g.number}>
                  <td><strong>{g.number}</strong></td>
                  <td>{g.course || "—"}</td>
                  <td>{g.specialty || "—"}</td>
                  <td>{g.faculty || "—"}</td>
                  <td><span className="status-dot-pill">{g.studentsCount ?? (g.students || []).length}</span></td>
                  <td className="admin-row-actions">
                    <button type="button" className="btn-ghost" onClick={() => fillGroupFormByNumber(g.number)} title="Редактировать" aria-label={`Редактировать группу ${g.number}`}><AdminActionIcon name="edit" /></button>
                    <button type="button" className="btn-danger" onClick={() => setDeleteTarget(g)} title="Удалить" aria-label={`Удалить группу ${g.number}`}><AdminActionIcon name="trash" /></button>
                  </td>
                </tr>
              ))}
              {pageRows.length === 0 ? <tr><td colSpan={6} className="sub">Группы не найдены.</td></tr> : null}
            </tbody>
          </table>
          {renderPager(rows.length, groupPage, setGroupPage)}
        </section>
        <aside className="card admin-side-editor">
          <div className="admin-breadcrumbs">Группы &gt; {gForm.faculty || "Факультет"} &gt; {gForm.specialty || "Специальность"}</div>
          <h3>{gForm.editTarget ? `Группа ${gForm.editTarget}` : "Новая группа"}</h3>
          <label>Название группы<input placeholder="Например, ИС-21" value={gForm.number} onChange={(e) => setGForm((p) => ({ ...p, number: e.target.value }))} /></label>
          <label>Курс<select value={gForm.course} onChange={(e) => setGForm((p) => ({ ...p, course: e.target.value }))}>
            {[1, 2, 3, 4].map((course) => <option key={course} value={`${course} курс`}>{course} курс</option>)}
          </select></label>
          <label>Факультет<select value={gForm.faculty} onChange={(e) => {
            const faculty = e.target.value;
            setGForm((p) => ({ ...p, faculty, specialty: specialtiesCatalog[faculty]?.[0] || "" }));
          }}>
            <option value="">Факультет</option>
            {facultiesCatalog.map((faculty) => <option key={faculty} value={faculty}>{faculty}</option>)}
          </select></label>
          <label>Специальность<select value={gForm.specialty} onChange={(e) => setGForm((p) => ({ ...p, specialty: e.target.value }))}>
            <option value="">Специальность</option>
            {activeSpecialties.map((specialty) => <option key={specialty} value={specialty}>{specialty}</option>)}
          </select></label>
          <label>Куратор<select value={gForm.curator || ""} onChange={(e) => setGForm((p) => ({ ...p, curator: e.target.value }))}>
            <option value="">Не назначен</option>
            {teacherOptions.map((teacher) => {
              const fullName = UiUtils.fullName(teacher);
              return <option key={teacher.id || fullName} value={fullName}>{fullName}</option>;
            })}
          </select></label>
          <div className="row">
            <button type="button" className="btn-main" onClick={saveWithWarning}>{gForm.editTarget ? "Сохранить" : "Создать группу"}</button>
            {gForm.editTarget ? <button type="button" className="btn-danger" onClick={() => activeGroup && setDeleteTarget(activeGroup)}>Удалить</button> : null}
          </div>
          {gForm.editTarget && (
            <section className="group-student-picker">
              <h4>Студенты</h4>
              {(activeGroup?.students || []).slice(0, 8).map((name) => <span key={name} className="teacher-chip">{name}</span>)}
              {(activeGroup?.students || []).length === 0 ? <p className="sub">В группе пока нет студентов.</p> : null}
              <input placeholder="Поиск студента без группы" value={studentQuery} onChange={(e) => setStudentQuery(e.target.value)} />
              <div className="group-student-options">
                {studentOptions.map((student) => {
                  const name = UiUtils.fullName(student);
                  const checked = selectedStudentNames.includes(name);
                  return (
                    <label key={student.id} className="group-student-option" draggable onDragStart={() => setDragStudentName(name)}>
                      <input type="checkbox" checked={checked} onChange={() => setSelectedStudentNames((prev) => checked ? prev.filter((item) => item !== name) : [...prev, name])} />
                      <span>{name}</span>
                      <small>{studentMetaOf(name).studentStatus || "Без группы"}</small>
                    </label>
                  );
                })}
              </div>
              <div className="admin-transfer-drop-hint" onDragOver={(e) => e.preventDefault()} onDrop={(e) => { e.preventDefault(); if (dragStudentName) addStudentsToGroup([dragStudentName]); }}>
                Перетащите сюда студента или выберите из списка
              </div>
              <button type="button" className="btn-ghost" onClick={() => addStudentsToGroup()}>+ Добавить студента</button>
            </section>
          )}
          <button type="button" className="btn-ghost" onClick={() => setAdminTab("students")}>Перейти к студентам</button>
        </aside>
      </div>
      {deleteTarget && (
        <div className="note-modal-backdrop" role="dialog" aria-modal="true">
          <div className="note-modal danger-confirm-modal">
            <h3>Удалить группу {deleteTarget.number}?</h3>
            <p>Студенты не будут удалены из системы.</p>
            <div className="danger-summary">
              <strong>Будут затронуты:</strong>
              <span>{deleteTarget.studentsCount ?? (deleteTarget.students || []).length} студентов будут отвязаны от группы</span>
            </div>
            <div className="note-modal-actions">
              <button type="button" className="btn-danger" onClick={confirmDelete}>Удалить</button>
              <button type="button" className="btn-ghost" onClick={() => setDeleteTarget(null)}>Отмена</button>
            </div>
          </div>
        </div>
      )}
    </section>
  );
}