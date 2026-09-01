package com.testlyflow.service;

import com.testlyflow.config.TestlyProperties;
import com.testlyflow.dto.AdminCategoryDto;
import com.testlyflow.dto.CategoryDto;
import com.testlyflow.dto.CategoryUpsertRequest;
import com.testlyflow.entity.Category;
import com.testlyflow.exception.ConflictException;
import com.testlyflow.exception.NotFoundException;
import com.testlyflow.dto.PrepLinkDto;
import com.testlyflow.repository.CategoryRepository;
import com.testlyflow.repository.PrepLinkRepository;
import com.testlyflow.repository.QuestionRepository;
import com.testlyflow.repository.TestRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final QuestionRepository questionRepository;
    private final TestRepository testRepository;
    private final PrepLinkRepository prepLinkRepository;
    private final TestlyProperties properties;

    public CategoryService(CategoryRepository categoryRepository,
                            QuestionRepository questionRepository,
                            TestRepository testRepository,
                            PrepLinkRepository prepLinkRepository,
                            TestlyProperties properties) {
        this.categoryRepository = categoryRepository;
        this.questionRepository = questionRepository;
        this.testRepository = testRepository;
        this.prepLinkRepository = prepLinkRepository;
        this.properties = properties;
    }

    @Transactional(readOnly = true)
    public List<CategoryDto> listPublic() {
        return categoryRepository.findAllByOrderBySortOrderAscNameAsc().stream()
                .map(c -> new CategoryDto(c.getId(), c.getName(), c.getDescription(), c.getColor(),
                        questionRepository.countByCategoryId(c.getId()), prepLinksOf(c.getId())))
                .filter(dto -> dto.questionCount() > 0)
                .toList();
    }

    /**
     * Estimate of how many questions a newly started attempt will contain, using each
     * category's own min/max when set and the global sampling defaults otherwise.
     */
    @Transactional(readOnly = true)
    public AttemptSizeEstimate estimateAttemptSize() {
        int min = 0;
        int max = 0;
        int blocks = 0;
        for (Category category : categoryRepository.findAllByOrderBySortOrderAscNameAsc()) {
            long count = questionRepository.countByCategoryId(category.getId());
            if (count <= 0) {
                continue;
            }
            blocks++;
            int catMin = category.getQuestionsMin() != null
                    ? category.getQuestionsMin()
                    : properties.getSampling().getQuestionsPerCategoryMin();
            int catMax = category.getQuestionsMax() != null
                    ? category.getQuestionsMax()
                    : properties.getSampling().getQuestionsPerCategoryMax();
            if (catMax < catMin) {
                catMax = catMin;
            }
            min += (int) Math.min(count, catMin);
            max += (int) Math.min(count, catMax);
        }
        return new AttemptSizeEstimate(min, max, blocks);
    }

    @Transactional(readOnly = true)
    public List<PrepLinkDto> listPrepLinks(Long categoryId) {
        return prepLinksOf(categoryId);
    }

    @Transactional(readOnly = true)
    public Map<Long, List<PrepLinkDto>> prepLinksByCategory() {
        return prepLinkRepository.findAll().stream()
                .collect(Collectors.groupingBy(
                        link -> link.getCategory().getId(),
                        Collectors.mapping(
                                link -> new PrepLinkDto(link.getId(), link.getTitle(), link.getUrl()),
                                Collectors.toList())));
    }

    private List<PrepLinkDto> prepLinksOf(Long categoryId) {
        return prepLinkRepository.findByCategoryIdOrderBySortOrderAsc(categoryId).stream()
                .map(l -> new PrepLinkDto(l.getId(), l.getTitle(), l.getUrl()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AdminCategoryDto> listAdmin() {
        return categoryRepository.findAllByOrderBySortOrderAscNameAsc().stream()
                .map(this::toAdminDto)
                .toList();
    }

    @Transactional
    public AdminCategoryDto create(CategoryUpsertRequest request) {
        String trimmedName = request.name().trim();
        categoryRepository.findByNameIgnoreCaseTrimmed(trimmedName).ifPresent(existing -> {
            throw new ConflictException(
                    "Категория \"" + existing.getName() + "\" уже существует (id=" + existing.getId() + ")");
        });

        Category category = new Category();
        applyRequest(category, request, trimmedName);
        category = categoryRepository.save(category);
        return toAdminDto(category);
    }

    @Transactional
    public AdminCategoryDto update(Long id, CategoryUpsertRequest request) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Категория с id=" + id + " не найдена"));

        String trimmedName = request.name().trim();
        categoryRepository.findByNameIgnoreCaseTrimmed(trimmedName).ifPresent(existing -> {
            if (!existing.getId().equals(id)) {
                throw new ConflictException(
                        "Категория \"" + existing.getName() + "\" уже существует (id=" + existing.getId() + ")");
            }
        });

        applyRequest(category, request, trimmedName);
        category = categoryRepository.save(category);
        return toAdminDto(category);
    }

    @Transactional
    public void delete(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Категория с id=" + id + " не найдена"));

        long questionCount = questionRepository.countByCategoryId(id);
        if (questionCount > 0) {
            throw new ConflictException(
                    "В категории \"" + category.getName() + "\" есть вопросы (" + questionCount
                            + "): сначала удалите или перенесите их");
        }
        categoryRepository.delete(category);
    }

    /**
     * Resolves the category to attach an uploaded file's questions to, either an existing one
     * by id, or a brand new one. Enforces that exactly one of the two ways is used.
     */
    @Transactional
    public CategoryResolution resolveForUpload(Long categoryId, String newCategoryName,
                                                String newCategoryDescription, String newCategoryColor) {
        boolean hasExisting = categoryId != null;
        boolean hasNew = newCategoryName != null && !newCategoryName.isBlank();

        if (!hasExisting && !hasNew) {
            throw new IllegalArgumentException(
                    "Укажите категорию: выберите существующую или создайте новую");
        }
        if (hasExisting && hasNew) {
            throw new IllegalArgumentException(
                    "Укажите только один способ: существующую категорию или создание новой, не оба сразу");
        }

        if (hasExisting) {
            Category category = categoryRepository.findById(categoryId)
                    .orElseThrow(() -> new NotFoundException("Категория с id=" + categoryId + " не найдена"));
            return new CategoryResolution(category, false);
        }

        String trimmedName = newCategoryName.trim();
        var duplicate = categoryRepository.findByNameIgnoreCaseTrimmed(trimmedName);
        if (duplicate.isPresent()) {
            throw new IllegalArgumentException(
                    "Категория \"" + duplicate.get().getName() + "\" уже существует: используйте categoryId="
                            + duplicate.get().getId());
        }

        Category category = new Category();
        category.setName(trimmedName);
        category.setSlug(uniqueSlug(trimmedName));
        category.setDescription(newCategoryDescription);
        category.setColor(newCategoryColor);
        category = categoryRepository.save(category);
        return new CategoryResolution(category, true);
    }

    private void applyRequest(Category category, CategoryUpsertRequest request, String trimmedName) {
        boolean nameChanged = !trimmedName.equals(category.getName());
        category.setName(trimmedName);
        if (category.getSlug() == null || nameChanged) {
            category.setSlug(uniqueSlug(trimmedName));
        }
        category.setDescription(request.description());
        category.setColor(request.color());
        if (request.sortOrder() != null) {
            category.setSortOrder(request.sortOrder());
        }
        category.setQuestionsMin(request.questionsMin());
        category.setQuestionsMax(request.questionsMax());
    }

    private String uniqueSlug(String name) {
        String base = SlugGenerator.slugify(name);
        String slug = base;
        int suffix = 2;
        while (categoryRepository.findBySlug(slug).isPresent()) {
            slug = base + "-" + suffix++;
        }
        return slug;
    }

    private AdminCategoryDto toAdminDto(Category category) {
        return new AdminCategoryDto(
                category.getId(),
                category.getName(),
                category.getSlug(),
                category.getDescription(),
                category.getColor(),
                questionRepository.countByCategoryId(category.getId()),
                testRepository.countByCategoryId(category.getId()),
                category.getSortOrder(),
                category.getQuestionsMin(),
                category.getQuestionsMax());
    }

    @Transactional
    public void updatePrepLinks(Long categoryId, List<com.testlyflow.dto.PrepLinkUpsertRequest> links) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new NotFoundException("Категория с id=" + categoryId + " не найдена"));

        prepLinkRepository.deleteByCategoryId(categoryId);

        int order = 0;
        for (com.testlyflow.dto.PrepLinkUpsertRequest linkRequest : links) {
            com.testlyflow.entity.PrepLink link = new com.testlyflow.entity.PrepLink();
            link.setCategory(category);
            link.setTitle(linkRequest.title());
            link.setUrl(linkRequest.url());
            link.setSortOrder(order++);
            prepLinkRepository.save(link);
        }
    }

    public record CategoryResolution(Category category, boolean created) {
    }

    public record AttemptSizeEstimate(int minQuestions, int maxQuestions, int blockCount) {
    }
}
