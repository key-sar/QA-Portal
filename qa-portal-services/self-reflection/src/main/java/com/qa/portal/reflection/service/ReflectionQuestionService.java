package com.qa.portal.reflection.service;

import java.util.Set;
import java.util.stream.Collectors;

import javax.transaction.Transactional;

import com.qa.portal.common.persistence.repository.QaCohortRepository;
import com.qa.portal.reflection.dto.QuestionDto;
import com.qa.portal.reflection.persistence.repository.CohortQuestionRepository;
import com.qa.portal.reflection.service.mapper.ReflectionQuestionMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.qa.portal.common.exception.QaResourceNotFoundException;
import com.qa.portal.common.security.QaSecurityContext;
import com.qa.portal.reflection.dto.ReflectionQuestionDto;
import com.qa.portal.reflection.persistence.entity.ReflectionQuestionEntity;
import com.qa.portal.reflection.persistence.repository.ReflectionQuestionRepository;

@Service
public class ReflectionQuestionService {
	private final Logger LOGGER = LoggerFactory.getLogger(ReflectionQuestionService.class);

	private ReflectionQuestionRepository reflectionQuestionRepo;

	private CohortQuestionRepository cohortQuestionRepository;

	private QaCohortRepository cohortRepository;

	private ReflectionQuestionMapper reflectionQuestionMapper;

	private QaSecurityContext context;

	@Autowired
	public ReflectionQuestionService(ReflectionQuestionRepository reflectionQuestionRepo,
									 CohortQuestionRepository cohortQuestionRepository,
									 QaCohortRepository cohortRepository,
									 ReflectionQuestionMapper reflectionQuestionMapper,
									 QaSecurityContext context) {
		this.reflectionQuestionRepo = reflectionQuestionRepo;
		this.cohortQuestionRepository = cohortQuestionRepository;
		this.cohortRepository = cohortRepository;
		this.reflectionQuestionMapper = reflectionQuestionMapper;
		this.context = context;
	}

	@Transactional
	public Set<ReflectionQuestionDto> getReflectionQuestionsByReflectionId(Integer id) {
		return this.reflectionQuestionRepo.findByReflectionId(id)
				.stream().map(this.reflectionQuestionMapper::mapToReflectionQuestionDto)
				.collect(Collectors.toSet());
	}
	
	@Transactional
	public Set<ReflectionQuestionDto> updateReflectionQuestions(Set<ReflectionQuestionDto> reflectionQuestions) {
		return reflectionQuestions.stream()
		.map(rqdto -> {
			ReflectionQuestionEntity reflectionQuestionToUpdate = this.reflectionQuestionRepo.findById(rqdto.getId())
					.orElseThrow(() -> new QaResourceNotFoundException("Reflection Question not found"));
			ReflectionQuestionEntity reflectionQuestionToUpdateFrom = this.reflectionQuestionMapper.mapToReflectionQuestionEntity(rqdto);
			reflectionQuestionToUpdate.setResponse(reflectionQuestionToUpdateFrom.getResponse());
			reflectionQuestionToUpdate.setTrainerResponse(reflectionQuestionToUpdateFrom.getTrainerResponse());
			reflectionQuestionToUpdate.setLastUpdatedBy(context.getUserName());
			return this.reflectionQuestionMapper.mapToReflectionQuestionDto(this.reflectionQuestionRepo.save(reflectionQuestionToUpdate));
		})
		.collect(Collectors.toSet());
	}

	@Transactional
	public Set<QuestionDto> getReflectionQuestionsByCohort(String cohortName){
		LOGGER.info("Cohort name" + cohortName);
		return this.cohortQuestionRepository.findByCohort(this.cohortRepository.findByname(cohortName).orElseThrow(
				()-> new QaResourceNotFoundException("Cohort not found for supplied name")))
				.stream()
				.map((e) -> reflectionQuestionMapper.mapToQuestionDto(e.getQuestion()))
				.collect(Collectors.toSet());
	}

	@Transactional
	public Set<ReflectionQuestionDto> createReflectionQuestions(Set<ReflectionQuestionDto> reflectionQuestions) {
		return reflectionQuestions.stream().map(rqdto ->
					this.reflectionQuestionMapper
					.mapToReflectionQuestionDto(this.reflectionQuestionRepo
							.save(this.reflectionQuestionMapper.mapToReflectionQuestionEntity(rqdto)))
				).collect(Collectors.toSet());
	}
}
