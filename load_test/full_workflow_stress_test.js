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
  stages: [
    { duration: '1m', target: 100 },    // 1분 동안 100명
    { duration: '3m', target: 500 },    // 3분 동안 500명
    { duration: '5m', target: 1000 },    // 5분 동안 1000명
    { duration: '10m', target: 2000 },   // 10분 동안 2000명
    { duration: '5m', target: 0 },       // 5분 동안 0명으로 감소
  ],
  thresholds: {
    http_req_duration: ['p(95)<5000'],  // 95% 요청이 5초 이내
    http_req_failed: ['rate<0.15'],     // 에러율 15% 미만
    errors: ['rate<0.15'],
  },
};

const DOORIBANK_URL = __ENV.DOORIBANK_URL || 'http://localhost:8081';
const WOORIDOORI_URL = __ENV.WOORIDOORI_URL || 'http://localhost:8080';

// 테스트 시작 전에 실제 회원 데이터를 가져옵니다
export function setup() {
  console.log('=== DooriBank에서 실제 회원 데이터 가져오기 ===');
  
  const response = http.get(`${DOORIBANK_URL}/api/test/members`);
  
  if (response.status !== 200) {
    console.error(`회원 데이터 조회 실패: ${response.status}`);
    console.error(`응답 본문: ${response.body}`);
    return { members: [] };
  }
  
  try {
    const members = JSON.parse(response.body);
    console.log(`=== ${members.length}명의 실제 회원 데이터 로드 완료 ===`);
    return { members: members || [] };
  } catch (e) {
    console.error(`회원 데이터 파싱 실패: ${e.message}`);
    return { members: [] };
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
  const timestamp = Date.now();
  const randomSuffix = Math.random().toString(36).substring(7);
  const email = `${member.name}_${timestamp}_${randomSuffix}@loadtest.com`;
  
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
  
  const password = 'Test1234!';
  
  // ========== 1단계: 회원가입 ==========
  const signupPayload = JSON.stringify({
    id: email,
    password: password,
    name: member.name,
    phone: member.phone,
    birthDate: birthDate,
    birthBack: birthBack,
  });

  const signupRes = http.post(
    `${WOORIDOORI_URL}/auth/join`,
    signupPayload,
    {
      headers: { 'Content-Type': 'application/json' },
      timeout: '30s',
      tags: { name: 'Signup' },
    }
  );

  const signupSuccess = check(signupRes, {
    '회원가입 성공': (r) => r.status === 200,
  });

  if (!signupSuccess) {
    errorRate.add(1);
    console.log(`회원가입 실패: ${member.name} (${email}), 상태: ${signupRes.status}`);
    return;
  }

  sleep(1);

  // ========== 2단계: 로그인 ==========
  const loginPayload = JSON.stringify({
    memberId: email,
    password: password,
  });

  const loginRes = http.post(
    `${WOORIDOORI_URL}/auth/login`,
    loginPayload,
    {
      headers: { 'Content-Type': 'application/json' },
      timeout: '30s',
      tags: { name: 'Login' },
    }
  );

  const loginSuccess = check(loginRes, {
    '로그인 성공': (r) => r.status === 200,
  });

  if (!loginSuccess) {
    errorRate.add(1);
    console.log(`로그인 실패: ${email}, 상태: ${loginRes.status}`);
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
  // 카드 번호는 랜덤 생성 (16자리)
  const cardNum = Array.from({ length: 16 }, () => Math.floor(Math.random() * 10)).join('');
  const cardPw = String(Math.floor(Math.random() * 100)).padStart(2, '0'); // 2자리 비밀번호
  const expiryMmYy = `${String(Math.floor(Math.random() * 12) + 1).padStart(2, '0')}${String(Math.floor(Math.random() * 10) + 25)}`; // MMYY 형식
  const cardCvc = String(Math.floor(Math.random() * 1000)).padStart(3, '0'); // 3자리 CVC
  const cardAlias = `테스트카드_${__VU}`;

  const cardPayload = JSON.stringify({
    cardNum: cardNum,
    cardPw: cardPw,
    expiryMmYy: expiryMmYy,
    cardUserRegistNum: birthDate,
    cardUserRegistBack: birthBack,
    cardCvc: cardCvc,
    cardAlias: cardAlias,
  });

  const cardRes = http.patch(
    `${WOORIDOORI_URL}/card/putCard`,
    cardPayload,
    {
      headers: {
        'Content-Type': 'application/json',
        Authorization: `Bearer ${accessToken}`,
      },
      timeout: '30s',
      tags: { name: 'CardRegistration' },
    }
  );

  const cardSuccess = check(cardRes, {
    '카드 등록 성공': (r) => r.status === 200,
  });

  if (!cardSuccess) {
    errorRate.add(1);
    console.log(`카드 등록 실패: ${email}, 상태: ${cardRes.status}, 응답: ${cardRes.body.substring(0, 200)}`);
    // 카드 등록 실패해도 계속 진행 (목표 설정은 가능할 수 있음)
  }

  sleep(1);

  // ========== 4단계: 목표 설정 ==========
  const today = new Date();
  const currentMonth = today.getMonth() + 1;
  const currentYear = today.getFullYear();
  const goalStartDate = `${currentYear}-${String(currentMonth).padStart(2, '0')}-01`;

  const goalPayload = JSON.stringify({
    goalJob: 'EMPLOYEE', // 직장인
    goalStartDate: goalStartDate,
    goalIncome: '3000000', // 300만원
    previousGoalMoney: 2000000, // 200만원
    essentialCategories: ['FOOD', 'TRANSPORTATION'], // 식비, 교통
  });

  const goalRes = http.put(
    `${WOORIDOORI_URL}/goal/setgoal`,
    goalPayload,
    {
      headers: {
        'Content-Type': 'application/json',
        Authorization: `Bearer ${accessToken}`,
      },
      timeout: '30s',
      tags: { name: 'SetGoal' },
    }
  );

  const goalSuccess = check(goalRes, {
    '목표 설정 성공': (r) => r.status === 200,
  });

  if (!goalSuccess) {
    errorRate.add(1);
    console.log(`목표 설정 실패: ${email}, 상태: ${goalRes.status}, 응답: ${goalRes.body.substring(0, 200)}`);
  }

  sleep(1);

  // ========== 5단계: 소비 리포트 발행 (월말 시나리오) ==========
  // 리포트는 스케줄러가 자동으로 발행하지만, 리포트 조회 API를 호출하여 리포트가 준비되었는지 확인
  // 또는 리포트 발행을 트리거하는 API가 있다면 호출
  
  // 리포트 조회 (메인 페이지 조회로 리포트 정보 확인)
  const mainRes = http.get(
    `${WOORIDOORI_URL}/main`,
    {
      headers: {
        Authorization: `Bearer ${accessToken}`,
      },
      timeout: '30s',
      tags: { name: 'ReportCheck' },
    }
  );

  const reportSuccess = check(mainRes, {
    '리포트 조회 성공': (r) => r.status === 200,
  });

  if (!reportSuccess) {
    errorRate.add(1);
    console.log(`리포트 조회 실패: ${email}, 상태: ${mainRes.status}`);
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

