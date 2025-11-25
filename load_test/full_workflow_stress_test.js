// 우리두리 전체 워크플로우 스트레스 테스트
// 1. 두리뱅크에 내역이 있는 사람이 회원가입
// 2. 그 사람들이 한꺼번에 자신의 카드를 등록
// 3. 그달의 목표 등록
// 4. 하필 그날이 월말이라 소비리포트가 날라올 때임 -> 소비 리포트 발행
import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate } from 'k6/metrics';

const errorRate = new Rate('errors');

export const options = {
  setupTimeout: '5m',  // setup 함수 타임아웃 5분으로 증가
  stages: [
    { duration: '2m', target: 50 },     // 2분 동안 50명 (점진적 증가)
    { duration: '5m', target: 100 },    // 5분 동안 100명
    { duration: '5m', target: 200 },    // 5분 동안 200명
    { duration: '5m', target: 300 },    // 5분 동안 300명
    { duration: '5m', target: 0 },      // 5분 동안 0명으로 감소
  ],
  thresholds: {
    http_req_duration: ['p(95)<10000'], // 95% 요청이 10초 이내 (서버 과부하 고려)
    http_req_failed: ['rate<0.30'],     // 에러율 30% 미만 (타임아웃 고려)
    errors: ['rate<0.30'],
  },
};

// 환경 변수에서 URL 가져오기 (없으면 기본값 사용)
const DOORIBANK_URL = "http://113.198.66.75:18177";
const WOORIDOORI_URL = "http://172.16.1.120:8080" ;

// 테스트 시작 전에 실제 회원 데이터를 가져옵니다
export function setup() {
  console.log('=== DooriBank에서 실제 회원 데이터 가져오기 ===');
  console.log(`DooriBank URL: ${DOORIBANK_URL}`);
  
  const url = `${DOORIBANK_URL}/api/test/members`;
  console.log(`회원 데이터 조회 URL: ${url}`);
  
  let response;
  try {
    response = http.get(url, {
      timeout: '120s',  // 서버 과부하 시 타임아웃 증가 (2분)
      tags: { name: 'Setup_GetMembers' },
    });
    
    console.log(`응답 상태: ${response.status}`);
    console.log(`응답 본문 길이: ${response.body ? response.body.length : 0} bytes`);
    console.log(`응답 본문 (처음 500자): ${response.body ? response.body.substring(0, 500) : 'null'}`);
  } catch (e) {
    console.error(`HTTP 요청 실패: ${e.message}`);
    console.error(`에러 스택: ${e.stack}`);
    return { members: [] };
  }
  
  if (!response || response.status !== 200) {
    console.error(`회원 데이터 조회 실패: ${response ? response.status : 'no response'}`);
    console.error(`응답 본문: ${response ? response.body : 'no response body'}`);
    console.error(`응답 헤더: ${response ? JSON.stringify(response.headers) : 'no headers'}`);
    console.error(`에러 코드: ${response ? response.error_code : 'no error code'}`);
    return { members: [] };
  }
  
  if (!response.body || response.body.length === 0) {
    console.error(`응답 본문이 비어있습니다.`);
    return { members: [] };
  }
  
  // 응답 본문 정리 (앞뒤 공백 제거, BOM 제거)
  let bodyText = response.body.trim();
  if (bodyText.charCodeAt(0) === 0xFEFF) {
    bodyText = bodyText.slice(1); // UTF-8 BOM 제거
  }
  
  try {
    let parsedData = JSON.parse(bodyText);
    console.log(`파싱된 데이터 타입: ${Array.isArray(parsedData) ? 'Array' : typeof parsedData}`);
    
    let members = null;
    
    // 응답이 배열인 경우
    if (Array.isArray(parsedData)) {
      members = parsedData;
      console.log(`직접 배열로 응답됨: ${members.length}개`);
    }
    // 응답이 객체로 감싸져 있는 경우 (여러 가능성 체크)
    else if (typeof parsedData === 'object' && parsedData !== null) {
      // 가능한 키 이름들 확인
      if (Array.isArray(parsedData.data)) {
        members = parsedData.data;
        console.log(`data 키에서 배열 찾음: ${members.length}개`);
      } else if (Array.isArray(parsedData.members)) {
        members = parsedData.members;
        console.log(`members 키에서 배열 찾음: ${members.length}개`);
      } else if (Array.isArray(parsedData.result)) {
        members = parsedData.result;
        console.log(`result 키에서 배열 찾음: ${members.length}개`);
      } else if (Array.isArray(parsedData.resultData)) {
        members = parsedData.resultData;
        console.log(`resultData 키에서 배열 찾음: ${members.length}개`);
      } else {
        // 객체의 모든 키 확인
        const keys = Object.keys(parsedData);
        console.error(`응답이 배열이 아닙니다. 객체 키들: ${keys.join(', ')}`);
        console.error(`응답 구조 (처음 500자): ${JSON.stringify(parsedData).substring(0, 500)}`);
        return { members: [] };
      }
    } else {
      console.error(`응답이 배열도 객체도 아닙니다. 타입: ${typeof parsedData}`);
      return { members: [] };
    }
    
    // 최종 검증
    if (!members || !Array.isArray(members)) {
      console.error(`최종적으로 배열을 찾을 수 없습니다.`);
      return { members: [] };
    }
    
    if (members.length === 0) {
      console.warn(`회원 데이터 배열이 비어있습니다.`);
      return { members: [] };
    }
    
    console.log(`=== ${members.length}명의 실제 회원 데이터 로드 완료 ===`);
    if (members.length > 0) {
      console.log(`첫 번째 회원 데이터 예시: ${JSON.stringify(members[0])}`);
      // 계좌 정보가 있는 회원 수 확인
      const membersWithAccount = members.filter(m => m.accountNumber && m.accountPassword);
      console.log(`계좌 정보가 있는 회원: ${membersWithAccount.length}명 / 전체: ${members.length}명`);
      if (membersWithAccount.length > 0) {
        console.log(`계좌 정보 예시: accountNumber=${membersWithAccount[0].accountNumber}, accountPassword=${membersWithAccount[0].accountPassword}`);
        // 계좌 정보가 있는 회원만 사용
        return { members: membersWithAccount };
      } else {
        console.warn(`⚠️ 경고: 계좌 정보가 있는 회원이 없습니다. 모든 회원 데이터를 사용합니다.`);
        console.warn(`⚠️ 두리뱅크 서버를 재시작했는지 확인하세요.`);
      }
    }
    return { members: members };
  } catch (e) {
    console.error(`회원 데이터 파싱 실패: ${e.message}`);
    console.error(`에러 스택: ${e.stack}`);
    console.error(`파싱 시도한 본문 (처음 1000자): ${bodyText.substring(0, 1000)}`);
    console.error(`응답 본문 원본 (처음 200자): ${response.body.substring(0, 200)}`);
    return { members: [] };
  }
}

function getCardInfoForMember(memberName, birthDate, birthBack) {
  if (!memberName || !birthDate || !birthBack) {
    console.log(`[카드정보] 필수 파라미터 누락 - name: ${memberName}, birthDate: ${birthDate}, birthBack: ${birthBack}`);
    return null;
  }

  const query = `memberName=${encodeURIComponent(memberName)}&registNum=${birthDate}&registBack=${birthBack}`;
  const url = `${WOORIDOORI_URL}/test/card-info?${query}`;

  try {
    const response = http.get(url, {
      timeout: '30s',
      tags: { name: 'FetchCardInfo' },
    });

    if (response.status !== 200) {
      console.log(`[카드정보] 조회 실패 - status: ${response.status}, body: ${response.body ? response.body.substring(0, 200) : 'null'}`);
      return null;
    }

    const parsed = JSON.parse(response.body);
    if (!parsed || !parsed.resultData) {
      console.log(`[카드정보] resultData가 없습니다 - body: ${response.body ? response.body.substring(0, 200) : 'null'}`);
      return null;
    }

    return parsed.resultData;
  } catch (e) {
    console.log(`[카드정보] 요청 중 오류 - ${e.message}`);
    return null;
  }
}

export default function (data) {
  // 실제 회원 데이터가 없으면 스킵
  if (!data.members || data.members.length === 0) {
    console.log('실제 회원 데이터가 없습니다. 테스트를 건너뜁니다.');
    return;
  }
  
  // 현재 VU에 맞는 회원 데이터 선택 (순환 사용)
  const memberIndex = __VU % data.members.length;
  const member = data.members[memberIndex];
  
  // 이메일은 고유하게 생성 (같은 회원이 여러 번 가입할 수 있도록)
  // 한글 이름을 영문으로 변환하거나 제거하여 이메일 형식 검증 통과
  const timestamp = Date.now();
  const randomSuffix = Math.random().toString(36).substring(7);
  // 한글 제거: 이름의 첫 글자를 영문으로 변환하거나 전체 제거
  const nameHash = member.name ? Array.from(member.name).map(c => c.charCodeAt(0).toString(36)).join('').substring(0, 5) : 'user';
  const email = `user${timestamp}_${randomSuffix}_${nameHash}@loadtest.com`;
  
  // 주민번호 파싱 (7자리인 경우)
  let birthDate = null;
  let birthBack = null;
  if (member.memberRegistNum && member.memberRegistNum.length === 7) {
    birthDate = member.memberRegistNum.substring(0, 6);
    birthBack = member.memberRegistNum.substring(6, 7);
  } else {
    // 주민번호가 없거나 형식이 다르면 랜덤 생성
    birthDate = `9${Math.floor(Math.random() * 10)}${String(Math.floor(Math.random() * 12) + 1).padStart(2, '0')}${String(Math.floor(Math.random() * 28) + 1).padStart(2, '0')}`;
    birthBack = String(Math.floor(Math.random() * 4) + 1);
  }
  
  // 전화번호 정규화 (하이픈 제거, 숫자만 추출)
  let phone = member.phone || '';
  phone = phone.replace(/[^0-9]/g, ''); // 숫자만 추출
  
  // 필수 필드 검증
  if (!member.name || !phone || !birthDate || !birthBack) {
    console.error(`필수 필드 누락: name=${member.name}, phone=${phone}, birthDate=${birthDate}, birthBack=${birthBack}`);
    errorRate.add(1);
    return;
  }
  
  const password = 'Test1234!';
  
  // ========== 1단계: 회원가입 ==========
  const signupPayload = JSON.stringify({
    id: email,
    password: password,
    name: member.name,
    phone: phone,
    birthDate: birthDate,
    birthBack: birthBack,
  });
  
  console.log(`회원가입 요청: ${member.name}, phone: ${phone}, birthDate: ${birthDate}, birthBack: ${birthBack}`);

  const signupRes = http.post(
    `${WOORIDOORI_URL}/auth/join`,
    signupPayload,
    {
      headers: { 'Content-Type': 'application/json' },
      timeout: '60s',  // 서버 과부하 시 타임아웃 증가
      tags: { name: 'Signup' },
    }
  );

  const signupSuccess = check(signupRes, {
    '회원가입 성공': (r) => r.status === 200 || r.status === 201,
  });

  if (signupSuccess) {
    console.log(`회원가입 성공: ${member.name} (${email}), 상태: ${signupRes.status}`);
  } else {
    errorRate.add(1);
    const errorBody = signupRes.body ? signupRes.body.substring(0, 500) : '응답 본문 없음';
    console.log(`회원가입 실패: ${member.name} (${email}), 상태: ${signupRes.status}`);
    console.log(`요청 페이로드: ${signupPayload}`);
    console.log(`에러 응답: ${errorBody}`);
    // 400 에러는 요청 형식 문제이므로 계속 진행하지 않음
    if (signupRes.status === 400) {
      return;
    }
    // 다른 에러는 일단 계속 진행 (서버 에러일 수 있음)
  }

  sleep(1);

  // ========== 2단계: 로그인 ==========
  const loginPayload = JSON.stringify({
    id: email,  // LoginDto는 @JsonProperty("id")로 설정되어 있음
    password: password,
  });

  const loginRes = http.post(
    `${WOORIDOORI_URL}/auth/login`,
    loginPayload,
    {
      headers: { 'Content-Type': 'application/json' },
      timeout: '60s',  // 서버 과부하 시 타임아웃 증가
      tags: { name: 'Login' },
    }
  );

  const loginSuccess = check(loginRes, {
    '로그인 성공': (r) => r.status === 200,
  });

  if (loginSuccess) {
    console.log(`로그인 성공: ${email}, 상태: ${loginRes.status}`);
  } else {
    errorRate.add(1);
    const errorBody = loginRes.body ? loginRes.body.substring(0, 500) : '응답 본문 없음';
    console.log(`로그인 실패: ${email}, 상태: ${loginRes.status}`);
    console.log(`요청 페이로드: ${loginPayload}`);
    console.log(`에러 응답: ${errorBody}`);
    return;
  }

  let accessToken = null;
  try {
    const loginData = JSON.parse(loginRes.body);
    accessToken = loginData.resultData?.tokens?.accessToken;
  } catch (e) {
    errorRate.add(1);
    console.log(`로그인 응답 파싱 실패: ${e.message}`);
    return;
  }

  if (!accessToken) {
    errorRate.add(1);
    console.log(`액세스 토큰 없음: ${email}`);
    return;
  }

  sleep(1);

  // ========== 3단계: 카드 등록 ==========
  const cardInfo = getCardInfoForMember(member.name, birthDate, birthBack);

  if (!cardInfo) {
    console.log(`카드 등록 스킵: ${email} - 카드 원본 데이터 없음 (name: ${member.name}, birthDate: ${birthDate}, birthBack: ${birthBack})`);
    sleep(1);
  } else {
    const cardPayload = JSON.stringify({
      cardNum: cardInfo.cardNum,
      cardPw: cardInfo.cardPw,
      expiryMmYy: cardInfo.expiryMmYy,
      cardUserRegistNum: cardInfo.cardUserRegistNum,
      cardUserRegistBack: cardInfo.cardUserRegistBack,
      cardCvc: cardInfo.cardCvc,
      cardAlias: cardInfo.cardUserName ? `${cardInfo.cardUserName}_테스트카드` : `테스트카드_${__VU}`,
    });

    const cardRes = http.patch(
      `${WOORIDOORI_URL}/card/putCard`,
      cardPayload,
      {
        headers: {
          'Content-Type': 'application/json',
          Authorization: `Bearer ${accessToken}`,
        },
        timeout: '60s',  // 서버 과부하 시 타임아웃 증가
        tags: { name: 'CardRegistration' },
      }
    );

    const cardSuccess = check(cardRes, {
      '카드 등록 성공': (r) => r.status === 200,
    });

    if (cardSuccess) {
      console.log(`카드 등록 성공: ${email}, 상태: ${cardRes.status}`);
    } else {
      errorRate.add(1);
      const errorBody = cardRes.body ? cardRes.body.substring(0, 500) : '응답 본문 없음';
      console.log(`카드 등록 실패: ${email}, 상태: ${cardRes.status}, 응답: ${errorBody}`);
      console.log(`요청 페이로드: ${cardPayload}`);
      // 카드 등록 실패해도 계속 진행 (목표 설정은 가능할 수 있음)
    }
    
    sleep(1);
  }

  // ========== 4단계: 목표 설정 ==========
  const today = new Date();
  const currentMonth = today.getMonth() + 1;
  const currentYear = today.getFullYear();
  const goalStartDate = `${currentYear}-${String(currentMonth).padStart(2, '0')}-01`;

  // essentialCategories는 빈 배열로 보내는 것이 안전함
  // 프론트엔드와 동일한 형식으로 보내기
  const goalPayloadObj = {
    goalJob: 'SALARY', // 직장인 (JobType enum)
    goalStartDate: goalStartDate, // "YYYY-MM-DD" 형식 (LocalDate로 변환됨)
    goalIncome: '3000', // 월 수입 (String, 단위: 만원)
    previousGoalMoney: 200, // 목표 소비금액 (Integer, 단위: 만원)
    essentialCategories: [], // 필수 카테고리 (빈 배열로 보내기)
  };
  
  const goalPayload = JSON.stringify(goalPayloadObj);
  console.log(`목표 설정 요청 페이로드: ${goalPayload}`);

  const goalRes = http.put(
    `${WOORIDOORI_URL}/goal/setgoal`,
    goalPayload,
    {
      headers: {
        'Content-Type': 'application/json',
        Authorization: `Bearer ${accessToken}`,
      },
      timeout: '60s',  // 서버 과부하 시 타임아웃 증가
      tags: { name: 'SetGoal' },
    }
  );

  const goalSuccess = check(goalRes, {
    '목표 설정 성공': (r) => r.status === 200,
  });

  if (goalSuccess) {
    console.log(`목표 설정 성공: ${email}, 상태: ${goalRes.status}`);
  } else {
    errorRate.add(1);
    const errorBody = goalRes.body ? goalRes.body.substring(0, 500) : '응답 본문 없음';
    console.log(`목표 설정 실패: ${email}, 상태: ${goalRes.status}, 응답: ${errorBody}`);
    console.log(`요청 페이로드: ${goalPayload}`);
  }

  sleep(1);

  // ========== 5단계: 소비 리포트 발행 (월말 시나리오) ==========
  // 리포트는 스케줄러가 자동으로 발행하지만, 리포트 조회 API를 호출하여 리포트가 준비되었는지 확인
  // 대시보드 API로 리포트 정보 확인
  const dashboardRes = http.get(
    `${WOORIDOORI_URL}/goal/report`,
    {
      headers: {
        Authorization: `Bearer ${accessToken}`,
      },
      timeout: '60s',  // 서버 과부하 시 타임아웃 증가
      tags: { name: 'ReportCheck' },
    }
  );

  const reportSuccess = check(dashboardRes, {
    '리포트 조회 성공': (r) => r.status === 200,
  });

  if (reportSuccess) {
    console.log(`리포트 조회 성공: ${email}, 상태: ${dashboardRes.status}`);
  } else {
    errorRate.add(1);
    const errorBody = dashboardRes.body ? dashboardRes.body.substring(0, 500) : '응답 본문 없음';
    console.log(`리포트 조회 실패: ${email}, 상태: ${dashboardRes.status}, 응답: ${errorBody}`);
  }

  sleep(1);
}

export function handleSummary(data) {
  return {
    'stdout': textSummary(data, { indent: ' ', enableColors: true }),
    'summary.json': JSON.stringify(data),
  };
}

function textSummary(data, options) {
  let summary = '\n';
  summary += '='.repeat(60) + '\n';
  summary += '우리두리 전체 워크플로우 스트레스 테스트 결과\n';
  summary += '(실제 DooriBank 회원 데이터 사용)\n';
  summary += '='.repeat(60) + '\n';
  summary += `총 요청 수: ${data.metrics.http_reqs.values.count}\n`;
  summary += `성공률: ${((1 - data.metrics.http_req_failed.values.rate) * 100).toFixed(2)}%\n`;
  summary += `평균 응답 시간: ${data.metrics.http_req_duration.values.avg.toFixed(2)}ms\n`;
  summary += `최대 응답 시간: ${data.metrics.http_req_duration.values.max.toFixed(2)}ms\n`;
  summary += `95% 응답 시간: ${data.metrics.http_req_duration.values['p(95)'].toFixed(2)}ms\n`;
  summary += `에러율: ${(data.metrics.http_req_failed.values.rate * 100).toFixed(2)}%\n`;
  summary += '\n';
  summary += '단계별 통계:\n';
  
  // 각 태그별 통계 추출
  const tags = ['Signup', 'Login', 'CardRegistration', 'SetGoal', 'ReportCheck'];
  tags.forEach(tag => {
    const tagData = data.metrics.http_req_duration.values.tags[tag];
    if (tagData) {
      summary += `  ${tag}: 평균 ${tagData.avg.toFixed(2)}ms, 최대 ${tagData.max.toFixed(2)}ms\n`;
    }
  });
  
  summary += '='.repeat(60) + '\n';
  return summary;
}

