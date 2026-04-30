function MiniIcon({ name }) {
  const paths = {
    calendar: "M7 3v3M17 3v3M4 9h16M6 6h12a2 2 0 0 1 2 2v10a2 2 0 0 1-2 2H6a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2Z",
    grade: "M12 3l2.7 5.48L21 9.37l-4.5 4.38 1.06 6.2L12 17.1 6.44 19.95l1.06-6.2L3 9.37l6.3-.89L12 3Z",
    absence: "M4 12h16M6 6l12 12M18 6 6 18",
    group: "M16 20v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2M18 20v-2a4 4 0 0 0-2-3.46M15 4.7a4 4 0 1 1 0 7.8M12 7a4 4 0 1 1-8 0 4 4 0 0 1 8 0Z",
    course: "M4 7l8-4 8 4-8 4-8-4Zm4 3v4c0 2 1.8 3.5 4 3.5s4-1.5 4-3.5v-4M20 9v6",
    faculty: "M4 20h16M6 20V8l6-5 6 5v12M9 20v-7h6v7M8 10h.01M12 10h.01M16 10h.01"
  };
  return (
    <svg className="mini-svg-icon" viewBox="0 0 24 24" fill="none" aria-hidden>
      <path d={paths[name] || paths.group} stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" />
    </svg>
  );
}

export function StudentProfile({
  avatars,
  donutGradient,
  gradeTotalCount,
  myAvg,
  myFeedHomeFiltered,
  myGroupMembersHomeFiltered,
  myStudent,
  onEditAvatar,
  onOpenNewNote,
  onOpenStudentPreview,
  onStartEditPost,
  profileCourse,
  profileFaculty,
  profileSpecialty,
  rusWeekday,
  scheduleWithStatusFiltered,
  session,
  setStudentTab,
  shortStudentName,
  studentTheme,
  students,
  todayLessonsCount,
  onChangeStudentTheme,
  UiUtils
}) {
  const myAvatarSrc = myStudent ? (avatars[myStudent.id] || "") : "";
  const avatarInitials = (session.studentName || "")
    .trim()
    .split(/\s+/)
    .slice(0, 2)
    .map((part) => part[0]?.toUpperCase() || "")
    .join("");

  return (
    <div className="student-home-layout">
      <section className="student-dash-hero">
        <div className="student-theme-switcher" aria-label="Выбор темы">
          <button type="button" className={`student-theme-icon-btn${studentTheme === "pink" ? " active" : ""}`} onClick={() => onChangeStudentTheme("pink")} aria-label="Розовая тема" title="Розовая тема">☀</button>
          <button type="button" className={`student-theme-icon-btn${studentTheme === "blue" ? " active" : ""}`} onClick={() => onChangeStudentTheme("blue")} aria-label="Голубая тема" title="Голубая тема">❄</button>
          <button type="button" className={`student-theme-icon-btn${studentTheme === "black" ? " active" : ""}`} onClick={() => onChangeStudentTheme("black")} aria-label="Чёрная тема" title="Чёрная тема">☾</button>
        </div>
        <div className="student-dash-hero-grid">
          <div className="student-dash-avatar-block">
            <div className="student-dash-avatar-ring">
              {myAvatarSrc ? (
                <img className="student-dash-avatar-img" src={myAvatarSrc} alt="" />
              ) : (
                <span className="student-avatar-placeholder" aria-hidden>{avatarInitials || "ST"}</span>
              )}
            </div>
          </div>
          <div>
            <h2 className="student-dash-name">{session.studentName}</h2>
            <p className="student-dash-meta-line student-dash-meta-icons">
              <span className="hero-ico hero-ico--group" aria-hidden><MiniIcon name="group" /></span> Группа {myStudent?.groupNumber || "—"}
              <span className="meta-dot">•</span>
              <span className="hero-ico hero-ico--course" aria-hidden><MiniIcon name="course" /></span> {profileCourse}
            </p>
            <p className="student-dash-meta-line student-dash-meta-icons">
              <span className="hero-ico hero-ico--faculty" aria-hidden><MiniIcon name="faculty" /></span> {profileFaculty}{profileSpecialty && profileSpecialty !== "Специальность не указана" ? `, ${profileSpecialty}` : ""}
            </p>
            <p className="student-dash-birth">День рождения: {session.birthDateText}</p>
            <div className="student-dash-hero-actions">
              <button type="button" className="edu-pill-btn" onClick={onEditAvatar}>
                Редактировать профиль
              </button>
            </div>
          </div>
        </div>
      </section>

      <div className="student-stat-cards">
        <button type="button" className="student-stat-card student-stat-card--cal" onClick={() => setStudentTab("schedule")}>
          <span className="student-stat-ico" aria-hidden><MiniIcon name="calendar" /></span>
          <span>
            <span className="student-stat-title">Сегодня</span>
            <span className="student-stat-value">{todayLessonsCount} {todayLessonsCount === 1 ? "пара" : (todayLessonsCount >= 2 && todayLessonsCount <= 4 ? "пары" : "пар")}</span>
          </span>
        </button>
        <button type="button" className="student-stat-card student-stat-card--star" onClick={() => setStudentTab("grades")}>
          <span className="student-stat-ico" aria-hidden><MiniIcon name="grade" /></span>
          <span>
            <span className="student-stat-title">Средний балл</span>
            <span className="student-stat-value">{myAvg}</span>
          </span>
        </button>
        <button type="button" className="student-stat-card student-stat-card--abs" onClick={() => setStudentTab("absences")}>
          <span className="student-stat-ico" aria-hidden><MiniIcon name="absence" /></span>
          <span>
            <span className="student-stat-title">Пропуски</span>
            <span className="student-stat-value">{session.totalAbsenceHours} ч</span>
          </span>
        </button>
        <button type="button" className="student-stat-card student-stat-card--grp" onClick={() => setStudentTab("group")}>
          <span className="student-stat-ico" aria-hidden><MiniIcon name="group" /></span>
          <span>
            <span className="student-stat-title">Группа</span>
            <span className="student-stat-value">{myStudent?.groupNumber || "—"}</span>
          </span>
        </button>
      </div>

      <article className="edu-widget student-home-widget student-home-widget--group">
        <div className="edu-widget-head">
          <h2 className="edu-widget-title">Моя группа</h2>
          <span className="edu-widget-badge">№ {myStudent?.groupNumber || "—"}</span>
        </div>
        <div className="student-home-group-chips">
          {myGroupMembersHomeFiltered.length === 0 ? (
            <p className="edu-widget-empty">Нет совпадений или состав не указан.</p>
          ) : (
            myGroupMembersHomeFiltered.slice(0, 14).map((member, idx) => {
              const peer = students.find((s) => UiUtils.fullName(s) === member);
              const pid = peer?.id;
              return (
                <button
                  key={`${member}-${idx}`}
                  type="button"
                  className={`student-home-peer${member === session.studentName ? " is-me" : ""}`}
                  onClick={() => peer && onOpenStudentPreview(member)}
                  disabled={!peer}
                  title={member}
                >
                  <span className="student-home-peer-avatar">
                    <img src={pid ? (avatars[pid] || "") : ""} alt="" />
                  </span>
                  <span className="student-home-peer-name">{shortStudentName(member)}</span>
                </button>
              );
            })
          )}
        </div>
        <button type="button" className="edu-widget-link" onClick={() => setStudentTab("group")}>Весь состав группы →</button>
      </article>

      <article className="edu-widget student-home-widget student-home-widget--schedule">
        <div className="edu-widget-head">
          <h2 className="edu-widget-title">Расписание на сегодня</h2>
          <span className="edu-widget-badge">{rusWeekday}</span>
        </div>
        <ul className="edu-schedule-list">
          {scheduleWithStatusFiltered.map((l) => (
            <li key={`${l.day}-${l.slot}-${l.discipline}-${l.lessonType || ""}`} className="edu-schedule-row">
              <div className="edu-schedule-time">{l.slot} пара · {l.time.split("-")[0]?.trim()}</div>
              <div className="edu-schedule-main">
                <div className="edu-schedule-subj">
                  {l.discipline}
                  {l.typeMeta ? <span className={`lesson-type-tag lesson-type-tag--${l.typeMeta.key}`}>{l.typeMeta.label}</span> : null}
                </div>
                <div className="edu-schedule-meta">{l.teacher} · ауд. {l.room}</div>
              </div>
              <span className={`edu-lesson-status edu-lesson-status--${l.status}`}>
                {l.status === "completed" ? "Завершено" : ""}
                {l.status === "current" ? "Сейчас" : ""}
                {l.status === "next" ? "Далее" : ""}
                {l.status === "upcoming" ? "Скоро" : ""}
              </span>
            </li>
          ))}
        </ul>
        {scheduleWithStatusFiltered.length === 0 ? <p className="edu-widget-empty">На сегодня занятий нет или ничего не найдено.</p> : null}
        <button type="button" className="edu-widget-link" onClick={() => setStudentTab("schedule")}>Полное расписание →</button>
      </article>

      <div className="student-home-split">
        <article className="edu-widget student-home-widget student-home-widget--notes">
          <div className="edu-widget-head">
            <h2 className="edu-widget-title">Заметки</h2>
            <button type="button" className="edu-icon-add" onClick={onOpenNewNote} aria-label="Добавить заметку">+</button>
          </div>
          <ul className="edu-notes-preview">
            {myFeedHomeFiltered.slice(0, 3).map((post, idx) => (
              <li key={post.id}>
                <button type="button" className={`edu-note-preview edu-note-preview--v${idx % 3}`} onClick={() => { setStudentTab("notes"); onStartEditPost(post); }}>
                  <span className="edu-note-preview-title">{post.text.slice(0, 52)}{post.text.length > 52 ? "…" : ""}</span>
                  <span className="edu-note-preview-date">{post.createdAt}</span>
                </button>
              </li>
            ))}
          </ul>
          {myFeedHomeFiltered.length === 0 ? <p className="edu-widget-empty">Заметок нет или ничего не найдено.</p> : null}
          <button type="button" className="edu-widget-link" onClick={() => setStudentTab("notes")}>Все заметки →</button>
        </article>

        <article className="edu-widget student-home-widget student-home-widget--journal student-panel-unified">
          <div className="edu-widget-head">
            <h2 className="edu-widget-title">Успеваемость</h2>
          </div>
          <div className="student-home-journal-preview">
            <div className="student-home-journal-main">
              <div className="grade-donut-wrap student-home-donut student-home-donut--full">
                <div className="grade-donut" style={{ background: donutGradient }} />
                <div className="grade-donut-hole">
                  <span className="grade-donut-avg">{myAvg}</span>
                  <span className="grade-donut-sub">средний балл</span>
                </div>
              </div>
              <button type="button" className="btn-main student-home-journal-btn" onClick={() => setStudentTab("grades")}>Открыть журнал</button>
            </div>
            <ul className="student-home-journal-meta student-home-journal-meta--legend">
              <li><span className="grade-legend-dot grade-legend-dot--excellent" />9-10 — отлично</li>
              <li><span className="grade-legend-dot grade-legend-dot--good" />7-8 — хорошо</li>
              <li><span className="grade-legend-dot grade-legend-dot--satisfactory" />5-6 — удовлетворительно</li>
              <li><span className="grade-legend-dot grade-legend-dot--poor" />0-4 — неудовлетворительно</li>
            </ul>
          </div>
          {gradeTotalCount === 0 ? <p className="edu-widget-empty">Оценок пока нет.</p> : null}
        </article>
      </div>
    </div>
  );
}
