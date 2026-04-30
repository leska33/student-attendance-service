export function AdminJournalSection({
  adminJournalFilters,
  setAdminJournalFilters,
  groups,
  disciplines,
  teachers,
  students,
  UiUtils,
  adminJournalInsights,
  grades,
  absences,
  absenceForm,
  setAbsenceForm,
  journalPage,
  setJournalPage,
  paginate,
  renderPager,
  saveAdminJournalGrade,
  saveAdminJournalAbsence,
  gradeLessonMeta,
  ACADEMIC_HOURS_PER_ABSENCE
}) {
  const LESSON_TYPES = [
    { code: "ЛК", title: "Лекция" },
    { code: "ПЗ", title: "Практическое занятие" },
    { code: "ЛР", title: "Лабораторная работа" }
  ];
  const gradeMetaKey = (studentName, disciplineName) => `${studentName}__${disciplineName}`;
  const gradeRecordMetaKey = (grade) => (grade?.id ? `grade:${grade.id}` : gradeMetaKey(grade?.studentName, grade?.disciplineName));
  const gradeMetaOf = (grade) => {
    if (grade?.id) return gradeLessonMeta[gradeRecordMetaKey(grade)] || {};
    return gradeLessonMeta[gradeMetaKey(grade?.studentName, grade?.disciplineName)] || {};
  };
  const normalizeLessonType = (value) => LESSON_TYPES.some((type) => type.code === value) ? value : "ЛК";
  const lessonTypeTitle = (value) => LESSON_TYPES.find((type) => type.code === normalizeLessonType(value))?.title || "Лекция";
  const gradeNumbers = (raw) => String(raw ?? "")
    .split(",")
    .map((part) => Number(part.trim().replace(",", ".")))
    .filter((value) => !Number.isNaN(value));
  const averageValue = (items) => {
    const values = items.flatMap((item) => gradeNumbers(item.value));
    return values.length ? (values.reduce((sum, value) => sum + value, 0) / values.length).toFixed(1) : "—";
  };
  const valueTone = (value, type) => {
    if (String(value).includes("уваж")) return "excused";
    if (type === "absence" || String(value).toUpperCase().includes("Н")) return "miss";
    const values = gradeNumbers(value);
    const n = values.length ? values.reduce((sum, item) => sum + item, 0) / values.length : Number(value);
    if (Number.isNaN(n)) return "empty";
    if (n >= 8) return "good";
    if (n >= 5) return "warn";
    return "bad";
  };
  const mode = adminJournalFilters.type === "absence" ? "absence" : "grade";
  const selectedLessonType = normalizeLessonType(adminJournalFilters.lessonType);
  const selectedDate = adminJournalFilters.period || new Date().toISOString().slice(0, 10);
  const selectedDiscipline = adminJournalFilters.discipline || disciplines[0]?.name || "";
  const selectedTeacher = adminJournalFilters.teacher || disciplines.find((d) => d.name === selectedDiscipline)?.teacherName || "";
  const tableStudents = students
    .filter((student) => !adminJournalFilters.group || String(student.groupNumber) === String(adminJournalFilters.group))
    .filter((student) => !adminJournalFilters.student || UiUtils.fullName(student) === adminJournalFilters.student)
    .sort((a, b) => UiUtils.fullName(a).localeCompare(UiUtils.fullName(b), "ru"));
  const pageStudents = paginate(tableStudents, journalPage);
  const journalDateLabel = (shiftWeeks = 0) => {
    const base = new Date(`${selectedDate}T12:00:00`);
    base.setDate(base.getDate() - (shiftWeeks * 7));
    return base.toLocaleDateString("ru-RU", { day: "2-digit", month: "2-digit" });
  };
  const historyDates = [3, 2, 1].map((shift) => {
    const date = new Date(`${selectedDate}T12:00:00`);
    date.setDate(date.getDate() - (shift * 7));
    return date.toISOString().slice(0, 10);
  });
  const moveJournalFocus = (rowIndex) => {
    const next = document.querySelector(`[data-admin-journal-row="${rowIndex}"]`);
    if (next) {
      next.focus();
      next.select?.();
    }
  };
  return (
    <section className="admin-modern-page admin-journal-page">
      <div className="admin-page-head">
        <div>
          <h2>Журнал администратора</h2>
        </div>
      </div>
      <div className="admin-filter-card">
        <select value={adminJournalFilters.group} onChange={(e) => { setAdminJournalFilters((p) => ({ ...p, group: e.target.value })); setJournalPage(1); }}><option value="">Все группы</option>{groups.map((g) => <option key={g.id} value={g.number}>{g.number}</option>)}</select>
        <select value={adminJournalFilters.discipline} onChange={(e) => { setAdminJournalFilters((p) => ({ ...p, discipline: e.target.value })); setJournalPage(1); }}><option value="">Все предметы</option>{disciplines.map((d) => <option key={d.id} value={d.name}>{d.name}</option>)}</select>
        <select value={adminJournalFilters.teacher} onChange={(e) => { setAdminJournalFilters((p) => ({ ...p, teacher: e.target.value })); setJournalPage(1); }}><option value="">Все преподаватели</option>{teachers.map((t) => <option key={t.id} value={UiUtils.fullName(t)}>{UiUtils.fullName(t)}</option>)}</select>
        <select value={adminJournalFilters.student} onChange={(e) => { setAdminJournalFilters((p) => ({ ...p, student: e.target.value })); setJournalPage(1); }}><option value="">Все студенты</option>{students.map((s) => <option key={s.id} value={UiUtils.fullName(s)}>{UiUtils.fullName(s)}</option>)}</select>
        <select value={selectedLessonType} onChange={(e) => { setAdminJournalFilters((p) => ({ ...p, lessonType: e.target.value })); setJournalPage(1); }}>{LESSON_TYPES.map((type) => <option key={type.code} value={type.code}>{type.title}</option>)}</select>
        <input type="date" value={selectedDate} onChange={(e) => { setAdminJournalFilters((p) => ({ ...p, period: e.target.value })); setJournalPage(1); }} />
        <select value={mode} onChange={(e) => { setAdminJournalFilters((p) => ({ ...p, type: e.target.value })); setJournalPage(1); }}><option value="grade">Выставить оценку</option><option value="absence">Отметить пропуск</option></select>
        <button type="button" className="btn-ghost" onClick={() => { setAdminJournalFilters({ group: "", discipline: "", teacher: "", student: "", type: "grade", period: "", lessonType: "ЛК" }); setJournalPage(1); }}>Сбросить</button>
      </div>
      <div className="admin-insight-grid">
        <article className="admin-insight-card admin-insight-card--red"><strong>Группа с большим количеством пропусков</strong><span>{adminJournalInsights.absence}</span></article>
        <article className="admin-insight-card admin-insight-card--yellow"><strong>Самый низкий средний балл</strong><span>{adminJournalInsights.lowGroup}</span></article>
        <article className="admin-insight-card admin-insight-card--green"><strong>Самый высокий средний балл</strong><span>{adminJournalInsights.highGroup}</span></article>
      </div>
      <article className="edu-widget teacher-journal-board admin-journal-board">
        <div className="teacher-journal-board-head">
          <div>
            <strong>{adminJournalFilters.group || "Все группы"} — {selectedDiscipline || "Предмет не выбран"} — {lessonTypeTitle(selectedLessonType)}</strong>
            <span>{new Date(`${selectedDate}T12:00:00`).toLocaleDateString("ru-RU")}</span>
          </div>
          {mode === "absence" ? (
            <select
              className="teacher-journal-reason"
              value={absenceForm.reason || ""}
              onChange={(e) => setAbsenceForm((p) => ({ ...p, reason: e.target.value }))}
            >
              <option value="">Без уважительной причины</option>
              <option value="Болезнь">Болезнь</option>
              <option value="Мероприятие">Мероприятие</option>
              <option value="Семейные обстоятельства">Семейные обстоятельства</option>
            </select>
          ) : null}
        </div>
        <div className="teacher-journal-table-wrap">
          <table className="teacher-journal-live-table teacher-journal-modern-table">
            <thead>
              <tr>
                <th>№</th>
                <th>Студент</th>
                <th>{journalDateLabel(3)}</th>
                <th>{journalDateLabel(2)}</th>
                <th>{journalDateLabel(1)}</th>
                <th>{new Date(`${selectedDate}T12:00:00`).toLocaleDateString("ru-RU")}<span className="journal-today-mark">(Сегодня)</span></th>
                <th>{mode === "absence" ? "Всего пропусков" : "Средний балл"}</th>
              </tr>
            </thead>
            <tbody>
              {pageStudents.map((student, rowIdx) => {
                const studentName = UiUtils.fullName(student);
                const rowNumber = ((journalPage - 1) * 10) + rowIdx;
                const studentGrades = grades.filter((grade) => {
                  const meta = gradeMetaOf(grade);
                  return grade.studentName === studentName
                    && grade.disciplineName === selectedDiscipline
                    && normalizeLessonType(meta.lessonType) === selectedLessonType;
                });
                const studentAbsences = absences
                  .filter((row) => row.studentName === studentName && row.disciplineName === selectedDiscipline)
                  .filter((row) => normalizeLessonType(row.lessonType) === selectedLessonType);
                const currentGrades = studentGrades.filter((grade) => gradeMetaOf(grade).date === selectedDate);
                const currentAbsences = studentAbsences.filter((row) => row.date === selectedDate);
                const currentAbsenceHours = currentAbsences
                  .reduce((sum, row) => sum + (Number(row.count || 0) * ACADEMIC_HOURS_PER_ABSENCE) + Number(row.excusedHours || 0), 0);
                const currentAbsenceExcused = currentAbsences.some((row) => Number(row.excusedHours || 0) > 0 || row.reason);
                const historyValues = historyDates.map((date) => {
                  if (mode === "absence") {
                    const rows = studentAbsences.filter((row) => row.date === date);
                    const hours = rows.reduce((sum, row) => sum + (Number(row.count || 0) * ACADEMIC_HOURS_PER_ABSENCE) + Number(row.excusedHours || 0), 0);
                    const excused = rows.some((row) => Number(row.excusedHours || 0) > 0 || row.reason);
                    return hours ? `${hours} ч${excused ? " уваж" : ""}` : "";
                  }
                  return studentGrades
                    .filter((item) => gradeMetaOf(item).date === date)
                    .map((item) => item.value)
                    .filter(Boolean)
                    .join(", ");
                });
                const totalAbsenceHours = studentAbsences.reduce((sum, row) => sum + (Number(row.count || 0) * ACADEMIC_HOURS_PER_ABSENCE), 0);
                return (
                  <tr key={student.id || studentName}>
                    <td className="teacher-journal-row-num">{rowNumber + 1}</td>
                    <td><span className="teacher-journal-student-cell">{studentName}</span></td>
                    {historyValues.map((value, idx) => (
                      <td key={`${studentName}-${idx}`}>{value ? <span className={`journal-grade-chip journal-grade-chip--${valueTone(value, mode)}`}>{String(value).replace(" уваж", "")}</span> : null}</td>
                    ))}
                    <td>
                      <input
                        key={`${mode}-${selectedDiscipline}-${selectedLessonType}-${selectedDate}-${studentName}`}
                        className={`teacher-journal-select teacher-journal-select--${currentAbsenceExcused ? "excused" : valueTone(mode === "absence" ? currentAbsenceHours : currentGrades.map((grade) => grade.value).join(", "), mode)}`}
                        data-admin-journal-row={rowNumber}
                        defaultValue={mode === "absence" ? (currentAbsenceHours || "") : currentGrades.map((grade) => grade.value).filter(Boolean).join(", ")}
                        onKeyDown={(e) => {
                          if (e.key === "Enter") {
                            e.preventDefault();
                            const payload = {
                              studentName,
                              disciplineName: selectedDiscipline,
                              teacherName: selectedTeacher,
                              lessonType: selectedLessonType,
                              date: selectedDate,
                              value: e.currentTarget.value,
                              reason: absenceForm.reason || ""
                            };
                            if (mode === "absence") saveAdminJournalAbsence(payload);
                            else saveAdminJournalGrade(payload);
                          } else if (e.key === "ArrowDown" || e.key === "ArrowUp") {
                            e.preventDefault();
                            moveJournalFocus(rowNumber + (e.key === "ArrowDown" ? 1 : -1));
                          }
                        }}
                      />
                    </td>
                    <td><span className={`teacher-journal-avg teacher-journal-avg--${valueTone(mode === "absence" ? totalAbsenceHours : averageValue(studentGrades), mode)}`}>{mode === "absence" ? `${totalAbsenceHours} ч` : averageValue(studentGrades)}</span></td>
                  </tr>
                );
              })}
              {pageStudents.length === 0 ? <tr><td colSpan={7}>Нет студентов по выбранным фильтрам.</td></tr> : null}
            </tbody>
          </table>
          {renderPager(tableStudents.length, journalPage, setJournalPage)}
        </div>
      </article>
    </section>
  );
}
