package com.example.student.service;

import com.example.student.dto.DisciplineCreateDto;
import com.example.student.dto.DisciplineResponseDto;
import com.example.student.entity.Discipline;
import com.example.student.entity.Student;
import com.example.student.entity.Teacher;
import com.example.student.exception.ResourceNotFoundException;
import com.example.student.mapper.DisciplineMapper;
import com.example.student.repository.GradeRepository;
import com.example.student.repository.TeacherRepository;
import com.example.student.repository.DisciplineRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class DisciplineService {

    private static final String DISCIPLINE_NOT_FOUND = "Discipline not found";
    private static final String TEACHER_NOT_FOUND = "Teacher not found";
    private final DisciplineRepository disciplineRepository;
    private final TeacherRepository teacherRepository;
    private final GradeRepository gradeRepository;
    public DisciplineService(DisciplineRepository disciplineRepository,
                             TeacherRepository teacherRepository,
                             GradeRepository gradeRepository) {
        this.disciplineRepository = disciplineRepository;
        this.teacherRepository = teacherRepository;
        this.gradeRepository = gradeRepository;
    }

    public List<DisciplineResponseDto> getAllDisciplinesDtoLazy() {
        return disciplineRepository.findAll()
                .stream()
                .map(DisciplineMapper::toDto)
                .toList();
    }

    public List<DisciplineResponseDto> getAllDisciplinesDtoOptimized() {
        return disciplineRepository.findAllWithRelations()
                .stream()
                .map(DisciplineMapper::toDto)
                .toList();
    }

    public DisciplineResponseDto getDisciplineById(Long id) {
        Discipline discipline = disciplineRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(DISCIPLINE_NOT_FOUND));
        return DisciplineMapper.toDto(discipline);
    }

    @Transactional
    public DisciplineResponseDto createDiscipline(DisciplineCreateDto dto) {
        Discipline discipline = new Discipline();
        discipline.setName(dto.getName());
        if (dto.getTeacherId() != null) {
            Teacher teacher = teacherRepository.findById(dto.getTeacherId())
                    .orElseThrow(() -> new ResourceNotFoundException(TEACHER_NOT_FOUND));
            discipline.setTeacher(teacher);
        }
        Discipline saved = disciplineRepository.save(discipline);
        return DisciplineMapper.toDto(saved);
    }

    @Transactional
    public DisciplineResponseDto updateDiscipline(Long id, DisciplineCreateDto dto) {
        Discipline discipline = disciplineRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(DISCIPLINE_NOT_FOUND));

        discipline.setName(dto.getName());
        if (dto.getTeacherId() != null) {
            Teacher teacher = teacherRepository.findById(dto.getTeacherId())
                    .orElseThrow(() -> new ResourceNotFoundException(TEACHER_NOT_FOUND));
            discipline.setTeacher(teacher);
        } else {
            discipline.setTeacher(null);
        }
        Discipline updated = disciplineRepository.save(discipline);
        return DisciplineMapper.toDto(updated);
    }

    @Transactional
    public void deleteDiscipline(Long id) {
        Discipline discipline = disciplineRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(DISCIPLINE_NOT_FOUND));
        gradeRepository.deleteAllByDiscipline(discipline);
        if (discipline.getStudents() != null) {
            for (Student student : discipline.getStudents()) {
                student.getDisciplines().remove(discipline);
            }
            discipline.getStudents().clear();
        }
        if (discipline.getTeacher() != null) {
            Teacher teacher = discipline.getTeacher();
            discipline.setTeacher(null);
            if (teacher.getDisciplines() != null) {
                teacher.getDisciplines().remove(discipline);
            }
        }
        disciplineRepository.delete(discipline);
    }
}