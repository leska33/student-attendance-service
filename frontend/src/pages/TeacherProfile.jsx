function MiniIcon({ name }) {
  const paths = {
    calendar: "M7 3v3M17 3v3M4 9h16M6 6h12a2 2 0 0 1 2 2v10a2 2 0 0 1-2 2H6a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2Z",
    edit: "M4 20h4l10.5-10.5a2.1 2.1 0 0 0-3-3L5 17v3Zm12-14 3 3",
    group: "M16 20v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2M18 20v-2a4 4 0 0 0-2-3.46M15 4.7a4 4 0 1 1 0 7.8M12 7a4 4 0 1 1-8 0 4 4 0 0 1 8 0Z",
    open: "M7 17 17 7M9 7h8v8"
  };
  return (
    <svg className="mini-svg-icon" viewBox="0 0 24 24" fill="none" aria-hidden>
      <path d={paths[name] || paths.group} stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" />
    </svg>
  );
}

export function TeacherProfile({
  activeTeacherProfile,
  journalRingClass,
  onOpenJournal,
  onOpenProfileEditor,
  onChangeTeacherTheme,
  ruStudentsLabel,
  rusWeekday,
  session,
  setTeacherTab,
  teacherTheme,
  teacherDashboardGroups,
  teacherJournalRows,
  teacherLessonStatusRu,
  teacherScheduleWithStatus
}) {
  return (
    <div className="teacher-home">
      <section className="teacher-hero-banner">
        <div className="student-theme-switcher teacher-theme-switcher" aria-label="Выбор темы">
          <button type="button" className={`student-theme-icon-btn${teacherTheme === "pink" ? " active" : ""}`} onClick={() => onChangeTeacherTheme("pink")} aria-label="Розовая тема" title="Розовая тема">☀</button>
          <button type="button" className={`student-theme-icon-btn${teacherTheme === "blue" ? " active" : ""}`} onClick={() => onChangeTeacherTheme("blue")} aria-label="Голубая тема" title="Голубая тема">❄</button>
          <button type="button" className={`student-theme-icon-btn${teacherTheme === "black" ? " active" : ""}`} onClick={() => onChangeTeacherTheme("black")} aria-label="Чёрная тема" title="Чёрная тема">☾</button>
        </div>
        <div className="teacher-hero-col teacher-hero-col--info">
          <div className="teacher-hero-avatar-row">
            <div className="teacher-hero-avatar-wrap">
              <img src={activeTeacherProfile?.avatar || ""} alt="" />
            </div>
            <div className="teacher-hero-name-block">
              <h1 className="teacher-hero-name">{session.teacherName}</h1>
              <span className="teacher-hero-badge">Преподаватель</span>
              <span className="pill">{activeTeacherProfile?.workStatus || "Активен"}</span>
              <p className="teacher-hero-dept">{activeTeacherProfile?.department || "Кафедра не указана"}</p>
              <p className="teacher-hero-exp">Стаж работы: {activeTeacherProfile?.experienceYears || "—"}</p>
              <button type="button" className="edu-pill-btn teacher-profile-edit-pill" onClick={onOpenProfileEditor}>
                <span className="teacher-hero-edit-ico" aria-hidden><MiniIcon name="edit" /></span> Редактировать профиль
              </button>
            </div>
          </div>
        </div>
      </section>

      <div className="teacher-home-grid">
        <article className="edu-widget teacher-widget-schedule-today">
          <div className="edu-widget-head">
            <h2 className="edu-widget-title">Расписание на сегодня</h2>
            <button type="button" className="teacher-widget-link" onClick={() => setTeacherTab("schedule")}>Все расписание →</button>
          </div>
          <p className="teacher-widget-sub">{rusWeekday}</p>
          <ul className="teacher-today-list">
            {teacherScheduleWithStatus.map((l) => (
              <li key={`${l.day}-${l.slot}-${l.discipline}-${l.group}-${l.lessonType || ""}`} className={`teacher-today-row teacher-today-row--${l.status}`}>
                <span className="teacher-today-bar" aria-hidden />
                <div className="teacher-today-time">{l.displayTime}</div>
                <div className="teacher-today-main">
                  <div className="teacher-today-subj">
                    {l.discipline}
                    {l.typeMeta ? <span className={`lesson-type-tag lesson-type-tag--${l.typeMeta.key}`}>{l.typeMeta.label}</span> : null}
                  </div>
                  <div className="teacher-today-meta">Группа {l.group} · ауд. {l.room}</div>
                </div>
                <span className={`teacher-today-status teacher-today-status--${l.status}`}>{teacherLessonStatusRu(l.status)}</span>
              </li>
            ))}
          </ul>
          {teacherScheduleWithStatus.length === 0 ? <p className="edu-widget-empty">На сегодня пар нет.</p> : null}
          <button type="button" className="teacher-today-footer-btn" onClick={() => setTeacherTab("schedule")}>
            <span aria-hidden><MiniIcon name="calendar" /></span> Открыть расписание
          </button>
        </article>

        <article className="edu-widget teacher-widget-groups">
          <div className="edu-widget-head">
            <h2 className="edu-widget-title">Группы</h2>
            <button type="button" className="teacher-widget-link" onClick={() => setTeacherTab("journal")}>Все группы →</button>
          </div>
          <ul className="teacher-groups-list">
            {teacherDashboardGroups.map((row, idx) => (
              <li key={row.number}>
                <button type="button" className="teacher-group-row" onClick={() => onOpenJournal(row.number, row.subjectsLabel.split(", ")[0] || "", row.lessonType || "")}>
                  <span className={`teacher-group-ico tg-ico-${idx % 5}`} aria-hidden><MiniIcon name="group" /></span>
                  <div className="teacher-group-text">
                    <strong className="teacher-group-name">{row.number}</strong>
                    <span className="teacher-group-subj">{row.subjectsLabel}</span>
                  </div>
                  <span className="teacher-group-count">{ruStudentsLabel(row.studentCount)}</span>
                  <span className={`teacher-group-ring ${journalRingClass(row.avgNum)}`} title="Средний балл по вашим предметам">
                    <span className="teacher-group-ring-val">{row.avg}</span>
                  </span>
                  <span className="teacher-group-chevron" aria-hidden>›</span>
                </button>
              </li>
            ))}
          </ul>
          {teacherDashboardGroups.length === 0 ? <p className="edu-widget-empty">Нет групп в расписании.</p> : null}
        </article>

        <article className="edu-widget teacher-widget-journal-quick">
          <h2 className="edu-widget-title">Быстрый доступ к журналу</h2>
          <div className="teacher-journal-table-wrap">
            <table className="teacher-journal-quick-table">
              <thead>
                <tr>
                  <th>Группа</th>
                  <th>Предмет</th>
                  <th>Тип</th>
                  <th>Последнее занятие</th>
                  <th>Действие</th>
                </tr>
              </thead>
              <tbody>
                {teacherJournalRows.map((row) => (
                  <tr key={`${row.group}-${row.discipline}-${row.lessonType}`}>
                    <td>{row.group}</td>
                    <td>{row.discipline}</td>
                    <td><span className={`lesson-type-tag lesson-type-tag--${row.typeMeta.key}`}>{row.typeMeta.label}</span></td>
                    <td>{row.lastLesson}</td>
                    <td>
                      <button type="button" className="teacher-journal-pen" title="Открыть журнал" aria-label={`Журнал: ${row.group}, ${row.discipline}`} onClick={() => onOpenJournal(row.group, row.discipline, row.lessonType)}>
                        <MiniIcon name="open" />
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          {teacherJournalRows.length === 0 ? <p className="edu-widget-empty">Добавьте пары в расписание.</p> : null}
        </article>
      </div>
    </div>
  );
}
