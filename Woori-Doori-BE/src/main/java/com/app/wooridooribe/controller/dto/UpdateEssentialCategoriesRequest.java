package com.app.wooridooribe.controller.dto;

import com.app.wooridooribe.entity.type.CategoryType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "회원의 필수 카테고리 설정 요청 DTO")
public class UpdateEssentialCategoriesRequest {

    @Schema(description = "필수로 설정할 카테고리 목록", example = "[\"FOOD\", \"HOUSING\"]")
    private List<CategoryType> essentialCategories;
}


