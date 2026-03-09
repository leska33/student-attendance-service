package com.example.student.service;

import com.example.student.dto.DisciplineCreateDto;
import com.example.student.dto.DisciplineResponseDto;
import com.example.student.entity.Discipline;
import com.example.student.entity.Teacher;
import com.example.student.mapper.DisciplineMapper;
import com.example.student.repository.DisciplineRepository;
import com.example.student.repository.TeacherRepository;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DisciplineService {

    private final DisciplineRepository disciplineRepository;
    private final TeacherRepository teacherRepository;

    public DisciplineService(DisciplineRepository disciplineRepository,
                             TeacherRepository teacherRepository) {
        this.disciplineRepository = disciplineRepository;
        this.teacherRepository = teacherRepository;
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

    public Discipline createDiscipline(DisciplineCreateDto dto) {

        Discipline discipline = new Discipline();
        discipline.setName(dto.getName());

        if (dto.getTeacherId() != null) {
            Teacher teacher = teacherRepository.findById(dto.getTeacherId())
                    .orElseThrow(() -> new RuntimeException("Teacher not found"));
            discipline.setTeacher(teacher);
        }

        return disciplineRepository.save(discipline);
    }

    public void deleteDiscipline(Long id) {
        disciplineRepository.deleteById(id);
    }
}