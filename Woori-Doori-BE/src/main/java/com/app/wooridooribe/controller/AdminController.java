package com.app.wooridooribe.controller;

import com.app.wooridooribe.controller.dto.AdminCardCreateRequestDto;
import com.app.wooridooribe.controller.dto.AdminCardEditRequestDto;
import com.app.wooridooribe.controller.dto.AdminDiaryNotificationRequestDto;
import com.app.wooridooribe.controller.dto.AdminGrantAuthorityRequestDto;
import com.app.wooridooribe.controller.dto.AdminReportNotificationRequestDto;
import com.app.wooridooribe.controller.dto.ApiResponse;
import com.app.wooridooribe.controller.dto.UploadedFileInfoDto;
import com.app.wooridooribe.controller.dto.CardResponseDto;
import com.app.wooridooribe.controller.dto.MemberResponseDto;
import com.app.wooridooribe.controller.dto.NotificationSendRequestDto;
import com.app.wooridooribe.entity.Member;
import com.app.wooridooribe.exception.CustomException;
import com.app.wooridooribe.exception.ErrorCode;
import com.app.wooridooribe.entity.File;
import com.app.wooridooribe.repository.file.FileRepository;
import com.app.wooridooribe.repository.member.MemberRepository;
import com.app.wooridooribe.service.member.MemberService;
import com.app.wooridooribe.service.card.CardService;
import com.app.wooridooribe.service.sse.SseService;
import com.app.wooridooribe.service.s3FileService.S3FileService;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Tag(name = "관리자", description = "관리자 전용 API (ADMIN 권한 필요)")
@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
@Slf4j
@SecurityRequirement(name = "JWT Token")
public class AdminController {

    private final MemberService memberService;
    private final CardService cardService;
    private final SseService sseService;
    private final MemberRepository memberRepository;
    private final S3FileService s3FileService;
    private final FileRepository fileRepository;
    private final ObjectMapper objectMapper;

    @Operation(summary = "전체 회원 조회", description = "모든 회원 정보를 조회합니다 (관리자 전용)")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "권한 없음")
    @GetMapping("/members")
    public ResponseEntity<ApiResponse<List<MemberResponseDto>>> getAllMembers() {
        log.info("관리자 - 전체 회원 조회");
        List<MemberResponseDto> members = memberService.getAllMembers();
        return ResponseEntity.ok(ApiResponse.res(200, "사용자들을 정보를 불러왔습니다!", members));
    }

    @Operation(summary = "특정 회원 조회", description = "회원 ID로 특정 회원 정보를 조회합니다 (관리자 전용)")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "회원을 찾을 수 없음")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "권한 없음")
    @GetMapping("/members/{memberId}")
    public ResponseEntity<ApiResponse<MemberResponseDto>> getMemberById(
            @Parameter(description = "조회할 회원 ID", required = true) @PathVariable Long memberId) {
        log.info("관리자 - 회원 조회: {}", memberId);
        MemberResponseDto member = memberService.getMemberByIdForAdmin(memberId);
        return ResponseEntity.ok(ApiResponse.res(200, "사용자 정보를 불러왔습니다!", member));
    }

    @Operation(summary = "회원 권한 변경", description = "특정 회원의 권한을 USER 또는 ADMIN으로 변경합니다 (관리자 전용)")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "권한 변경 성공")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "회원을 찾을 수 없음")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "권한 없음")
    @PutMapping("/members/authority")
    public ResponseEntity<ApiResponse<MemberResponseDto>> updateMemberAuthority(
            @Parameter(description = "회원 권한 변경 요청 정보", required = true) @Valid @RequestBody AdminGrantAuthorityRequestDto requestDto) {
        log.info("관리자 - 회원 권한 변경 요청: memberId={}, authority={}", requestDto.getMemberId(), requestDto.getAuthority());
        MemberResponseDto updatedMember = memberService.updateMemberAuthority(requestDto.getMemberId(),
                requestDto.getAuthority());
        return ResponseEntity.ok(ApiResponse.res(200, "회원 권한이 성공적으로 변경되었습니다!", updatedMember));
    }

    @Operation(summary = "전체 카드 조회", description = "tbl_card에 등록된 모든 카드 정보를 조회합니다 (관리자 전용)")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "권한 없음")
    @GetMapping("/card")
    public ResponseEntity<ApiResponse<List<CardResponseDto>>> getAllCards() {
        log.info("관리자 - 전체 카드 조회");
        List<CardResponseDto> cards = cardService.getAllCards();
        return ResponseEntity.ok(ApiResponse.res(200, "카드 정보를 불러왔습니다!", cards));
    }

    @Operation(summary = "S3 카드 배너 이미지 업로드 테스트", description = "카드 배너 이미지를 S3의 card_banner 폴더에 업로드하여 테스트합니다 (관리자 전용)")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "업로드 성공")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "권한 없음")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "서버 오류")
    @PostMapping(value = "/upload/card-banner", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<UploadedFileInfoDto>> uploadCardBannerImage(
            @Parameter(description = "업로드할 카드 배너 이미지 파일", required = true) @RequestParam("file") MultipartFile file) {
        log.info("관리자 - S3 카드 배너 이미지 업로드 요청 수신: fileName={}, fileSize={}",
                file.getOriginalFilename(), file.getSize());

        try {
            UploadedFileInfoDto uploadedFile = s3FileService.uploadImage(file, "card_banner");
            log.info("관리자 - S3 카드 배너 이미지 업로드 성공: fileUrl={}, fileName={}",
                    uploadedFile.getFileUrl(), uploadedFile.getFileName());
            return ResponseEntity.ok(ApiResponse.res(200, "카드 배너 이미지가 성공적으로 업로드되었습니다!", uploadedFile));
        } catch (Exception e) {
            log.error("관리자 - S3 카드 배너 이미지 업로드 실패: {}", e.getMessage(), e);
            return ResponseEntity.status(500)
                    .body(ApiResponse.<UploadedFileInfoDto>builder()
                            .statusCode(500)
                            .errorResultMsg("카드 배너 이미지 업로드에 실패했습니다: " + e.getMessage())
                            .build());
        }
    }

    @Operation(summary = "S3 카드 이미지 업로드 테스트", description = "카드 이미지를 S3의 card_images 폴더에 업로드하여 테스트합니다 (관리자 전용)")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "업로드 성공")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "권한 없음")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "서버 오류")
    @PostMapping(value = "/upload/card-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<UploadedFileInfoDto>> uploadCardImage(
            @Parameter(description = "업로드할 카드 이미지 파일", required = true) @RequestParam("file") MultipartFile file) {
        log.info("관리자 - S3 카드 이미지 업로드 요청 수신: fileName={}, fileSize={}",
                file.getOriginalFilename(), file.getSize());

        try {
            UploadedFileInfoDto uploadedFile = s3FileService.uploadImage(file, "card_images");
            log.info("관리자 - S3 카드 이미지 업로드 성공: fileUrl={}, fileName={}",
                    uploadedFile.getFileUrl(), uploadedFile.getFileName());
            return ResponseEntity.ok(ApiResponse.res(200, "카드 이미지가 성공적으로 업로드되었습니다!", uploadedFile));
        } catch (Exception e) {
            log.error("관리자 - S3 카드 이미지 업로드 실패: {}", e.getMessage(), e);
            return ResponseEntity.status(500)
                    .body(ApiResponse.<UploadedFileInfoDto>builder()
                            .statusCode(500)
                            .errorResultMsg("카드 이미지 업로드에 실패했습니다: " + e.getMessage())
                            .build());
        }
    }

    @Operation(summary = "카드 신규 등록", description = "새로운 카드를 tbl_card에 등록합니다. 카드 이미지와 배너 이미지를 함께 업로드합니다 (관리자 전용)")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "등록 성공")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "권한 없음")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "서버 오류")
    @PostMapping(value = "/createCard", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<CardResponseDto>> createCard(
            @Parameter(description = "카드 이미지 파일", required = true) @RequestPart("cardImage") MultipartFile cardImage,
            @Parameter(description = "카드 배너 이미지 파일 (선택)") @RequestPart(value = "cardBanner", required = false) MultipartFile cardBanner,
            @Parameter(description = "카드 생성 요청 정보 (JSON 문자열)", required = true) @RequestPart("cardInfo") String cardInfoJson) {
        log.info("관리자 - 카드 생성 요청 수신: cardImage={}, cardBanner={}",
                cardImage != null ? cardImage.getOriginalFilename() : "null",
                cardBanner != null ? cardBanner.getOriginalFilename() : "null");

        try {
            // 필수 파라미터 검증
            if (cardImage == null || cardImage.isEmpty()) {
                throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
            }

            // JSON 문자열을 DTO로 변환
            AdminCardCreateRequestDto requestDto = objectMapper.readValue(cardInfoJson,
                    AdminCardCreateRequestDto.class);
            log.info("관리자 - 카드 생성 요청 파싱 완료: cardName={}", requestDto.getCardName());

            // 카드 이미지 S3 업로드 및 File 엔티티 생성
            UploadedFileInfoDto cardImageInfo = s3FileService.uploadImage(cardImage, "card_images");
            String cardImageOriginalName = cardImage.getOriginalFilename();
            if (cardImageOriginalName == null) {
                cardImageOriginalName = "card_image";
            }

            File cardImageFile = File.builder()
                    .uuid(cardImageInfo.getFileName())
                    .fileOriginName(cardImageOriginalName)
                    .filePath(cardImageInfo.getFileUrl())
                    .fileType(cardImage.getContentType() != null ? cardImage.getContentType() : "image/jpeg")
                    .build();
            File savedCardImageFile = fileRepository.save(cardImageFile);
            log.info("관리자 - 카드 이미지 업로드 및 File 엔티티 생성 완료: fileId={}", savedCardImageFile.getId());

            // 카드 배너 이미지 S3 업로드 및 File 엔티티 생성
            File savedCardBannerFile = null;
            if (cardBanner != null && !cardBanner.isEmpty()) {
                UploadedFileInfoDto cardBannerInfo = s3FileService.uploadImage(cardBanner, "card_banner");
                String cardBannerOriginalName = cardBanner.getOriginalFilename();
                if (cardBannerOriginalName == null) {
                    cardBannerOriginalName = "card_banner";
                }

                File cardBannerFile = File.builder()
                        .uuid(cardBannerInfo.getFileName())
                        .fileOriginName(cardBannerOriginalName)
                        .filePath(cardBannerInfo.getFileUrl())
                        .fileType(cardBanner.getContentType() != null ? cardBanner.getContentType() : "image/jpeg")
                        .build();
                savedCardBannerFile = fileRepository.save(cardBannerFile);
                log.info("관리자 - 카드 배너 이미지 업로드 및 File 엔티티 생성 완료: fileId={}", savedCardBannerFile.getId());
            }

            // File ID를 DTO에 설정
            AdminCardCreateRequestDto requestWithFileIds = AdminCardCreateRequestDto.builder()
                    .cardName(requestDto.getCardName())
                    .annualFee1(requestDto.getAnnualFee1())
                    .annualFee2(requestDto.getAnnualFee2())
                    .cardBenefit(requestDto.getCardBenefit())
                    .cardType(requestDto.getCardType())
                    .cardSvc(requestDto.getCardSvc())
                    .cardImageFileId(savedCardImageFile.getId())
                    .cardBannerFileId(savedCardBannerFile != null ? savedCardBannerFile.getId() : null)
                    .build();

            // 카드 생성
            CardResponseDto createdCard = cardService.createCardForAdmin(requestWithFileIds);
            log.info("관리자 - 카드 생성 완료: cardId={}, cardName={}", createdCard.getId(), createdCard.getCardName());
            return ResponseEntity.ok(ApiResponse.res(200, "카드가 성공적으로 등록되었습니다!", createdCard));

        } catch (IOException e) {
            log.error("관리자 - 카드 생성 실패: JSON 파싱 또는 파일 업로드 오류 - {}", e.getMessage(), e);
            return ResponseEntity.status(500)
                    .body(ApiResponse.<CardResponseDto>builder()
                            .statusCode(500)
                            .errorResultMsg("카드 생성에 실패했습니다: " + e.getMessage())
                            .build());
        } catch (Exception e) {
            log.error("관리자 - 카드 생성 실패: {}", e.getMessage(), e);
            return ResponseEntity.status(500)
                    .body(ApiResponse.<CardResponseDto>builder()
                            .statusCode(500)
                            .errorResultMsg("카드 생성에 실패했습니다: " + e.getMessage())
                            .build());
        }
    }

    @Operation(summary = "카드 정보 수정", description = "기존 카드의 정보를 수정합니다 (관리자 전용)")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "수정 성공")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "카드를 찾을 수 없음")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "권한 없음")
    @PutMapping("/editCard")
    public ResponseEntity<ApiResponse<CardResponseDto>> editCard(
            @Parameter(description = "카드 수정 요청 정보", required = true) @Valid @RequestBody AdminCardEditRequestDto requestDto) {
        log.info("관리자 - 카드 수정 요청 수신: cardId={}", requestDto.getCardId());
        CardResponseDto updatedCard = cardService.editCardForAdmin(requestDto);
        return ResponseEntity.ok(ApiResponse.res(200, "카드가 성공적으로 수정되었습니다!", updatedCard));
    }

    @Operation(summary = "카드 삭제", description = "카드를 삭제합니다. status를 UNABLE로 변경하여 soft delete를 수행합니다 (관리자 전용)")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "삭제 성공")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "카드를 찾을 수 없음")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "410", description = "이미 삭제된 카드")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "권한 없음")
    @DeleteMapping("/deleteCard/{cardId}")
    public ResponseEntity<ApiResponse<Void>> deleteCard(
            @Parameter(description = "삭제할 카드 ID", required = true) @PathVariable Long cardId) {
        log.info("관리자 - 카드 삭제 요청 수신: cardId={}", cardId);
        cardService.deleteCardForAdmin(cardId);
        return ResponseEntity.ok(ApiResponse.res(200, "카드가 성공적으로 삭제되었습니다."));
    }

    @Operation(summary = "특정 사용자에게 알림 전송", description = "특정 사용자에게 SSE를 통해 알림을 전송합니다 (관리자 전용)")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "알림 전송 성공")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "회원을 찾을 수 없음 또는 SSE 연결 없음")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "권한 없음")
    @PostMapping("/send/custom")
    public ResponseEntity<ApiResponse<Void>> sendNotification(
            @Parameter(description = "알림 전송 요청 정보", required = true) @Valid @RequestBody NotificationSendRequestDto requestDto) {
        log.info("관리자 - 알림 전송 요청: memberId(이메일)={}, message={}", requestDto.getMemberId(), requestDto.getMessage());

        // 이메일로 회원 찾기
        Member member = memberRepository.findByMemberId(requestDto.getMemberId())
                .orElseThrow(() -> {
                    log.warn("관리자 - 알림 전송 실패: 회원을 찾을 수 없음 - memberId(이메일)={}", requestDto.getMemberId());
                    return new CustomException(ErrorCode.USER_NOT_FOUND);
                });

        Long memberId = member.getId();
        log.info("관리자 - 회원 조회 성공: 이메일={}, DB ID={}, 이름={}",
                requestDto.getMemberId(), memberId, member.getMemberName());

        boolean sent = sseService.sendToUser(memberId, "message", requestDto.getMessage());

        if (sent) {
            log.info("관리자 - 알림 전송 성공: DB ID={}, 이메일={}, 메시지={}",
                    memberId, requestDto.getMemberId(), requestDto.getMessage());
            return ResponseEntity.ok(ApiResponse.res(200, "알림이 성공적으로 전송되었습니다."));
        } else {
            log.warn("관리자 - 알림 전송 실패: SSE 연결 없음 - DB ID={}, 이메일={}, 현재 연결된 사용자 수={}",
                    memberId, requestDto.getMemberId(), sseService.getConnectedUserCount());
            return ResponseEntity.status(404)
                    .body(ApiResponse.error(404,
                            String.format("해당 사용자(이메일: %s, DB ID: %d)가 SSE에 연결되어 있지 않습니다. 먼저 /sse/connect에 연결해주세요.",
                                    requestDto.getMemberId(), memberId)));
        }
    }

    @Operation(summary = "특정 사용자에게 일기 알림 전송", description = "특정 사용자에게 SSE를 통해 일기 알림을 전송합니다 (관리자 전용)")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "알림 전송 성공")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "회원을 찾을 수 없음 또는 SSE 연결 없음")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "권한 없음")
    @PostMapping("/send/diary")
    public ResponseEntity<ApiResponse<Void>> sendDiaryNotification(
            @Parameter(description = "일기 알림 전송 요청 정보", required = true) @Valid @RequestBody AdminDiaryNotificationRequestDto requestDto) {
        log.info("관리자 - 일기 알림 전송 요청: memberId(이메일)={}", requestDto.getMemberId());

        // 이메일로 회원 찾기
        Member member = memberRepository.findByMemberId(requestDto.getMemberId())
                .orElseThrow(() -> {
                    log.warn("관리자 - 일기 알림 전송 실패: 회원을 찾을 수 없음 - memberId(이메일)={}", requestDto.getMemberId());
                    return new CustomException(ErrorCode.USER_NOT_FOUND);
                });

        Long memberId = member.getId();
        log.info("관리자 - 회원 조회 성공: 이메일={}, DB ID={}, 이름={}",
                requestDto.getMemberId(), memberId, member.getMemberName());

        // SSE를 통해 일기 알림 전송
        sseService.sendDiaryNotification(memberId);

        log.info("관리자 - 일기 알림 전송 완료: DB ID={}, 이메일={}", memberId, requestDto.getMemberId());
        return ResponseEntity.ok(ApiResponse.res(200, "일기 알림이 성공적으로 전송되었습니다."));
    }

    @Operation(summary = "특정 사용자에게 리포트 알림 전송", description = "특정 사용자에게 SSE를 통해 리포트 알림을 전송합니다 (관리자 전용)")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "알림 전송 성공")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "회원을 찾을 수 없음 또는 SSE 연결 없음")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "권한 없음")
    @PostMapping("/send/report")
    public ResponseEntity<ApiResponse<Void>> sendReportNotification(
            @Parameter(description = "리포트 알림 전송 요청 정보", required = true) @Valid @RequestBody AdminReportNotificationRequestDto requestDto) {
        log.info("관리자 - 리포트 알림 전송 요청: memberId(이메일)={}", requestDto.getMemberId());

        // 이메일로 회원 찾기
        Member member = memberRepository.findByMemberId(requestDto.getMemberId())
                .orElseThrow(() -> {
                    log.warn("관리자 - 리포트 알림 전송 실패: 회원을 찾을 수 없음 - memberId(이메일)={}", requestDto.getMemberId());
                    return new CustomException(ErrorCode.USER_NOT_FOUND);
                });

        Long memberId = member.getId();
        log.info("관리자 - 회원 조회 성공: 이메일={}, DB ID={}, 이름={}",
                requestDto.getMemberId(), memberId, member.getMemberName());

        // 현재 월 가져오기
        int currentMonth = java.time.LocalDate.now().getMonthValue();

        // SSE를 통해 리포트 알림 전송 (현재 월 자동 사용)
        sseService.sendReportNotification(memberId, currentMonth);

        log.info("관리자 - 리포트 알림 전송 완료: DB ID={}, 이메일={}, month={}",
                memberId, requestDto.getMemberId(), currentMonth);
        return ResponseEntity.ok(ApiResponse.res(200, "리포트 알림이 성공적으로 전송되었습니다."));
    }
}
