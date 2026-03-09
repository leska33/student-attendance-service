INSERT INTO teacher (id, name) VALUES (1, 'Скиба Ирина Геннальевна');
INSERT INTO teacher (id, name) VALUES (2, 'Поденок Леонид Петрович');

INSERT INTO group_table (id, group_number) VALUES (1, '450502');
INSERT INTO group_table (id, group_number) VALUES (2, '450501');

INSERT INTO discipline (id, name, teacher_id) VALUES (1, 'ПнаЯВУ', 1);
INSERT INTO discipline (id, name, teacher_id) VALUES (2, 'ОСиСП', 2);

INSERT INTO student (id, full_name, group_id, attendance_count, average_grade)
VALUES (3580045, 'Лубочко Ульяна Ивановна', 1, 10, 8.5);

INSERT INTO student (id, full_name, group_id, attendance_count, average_grade)
VALUES (3580011, 'Басюк Алеся Владимировна', 2, 12, 7.5);

INSERT INTO student_disciplines (student_id, discipline_id)
VALUES (1,1);

INSERT INTO student_disciplines (student_id, discipline_id)
VALUES (2,2);

INSERT INTO grade (id, student_id, discipline_id, grade_value)
VALUES (1,1,1,5);

INSERT INTO grade (id, student_id, discipline_id, grade_value)
VALUES (2,2,2,4);