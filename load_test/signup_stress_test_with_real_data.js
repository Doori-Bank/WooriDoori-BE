// 우리두리 회원가입 스트레스 테스트 (실제 DooriBank 회원 데이터 사용)
import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate } from 'k6/metrics';

const errorRate = new Rate('errors');

export const options = {
  stages: [
    { duration: '30s', target: 50 },    // 30초 동안 50명
    { duration: '1m', target: 100 },     // 1분 동안 100명
    { duration: '2m', target: 200 },     // 2분 동안 200명
    { duration: '3m', target: 500 },     // 3분 동안 500명
    { duration: '5m', target: 1000 },    // 5분 동안 1000명
    { duration: '10m', target: 2000 },   // 10분 동안 2000명
    { duration: '5m', target: 0 },       // 5분 동안 0명으로 감소
  ],
  thresholds: {
    http_req_duration: ['p(95)<3000'],  // 95% 요청이 3초 이내
    http_req_failed: ['rate<0.1'],      // 에러율 10% 미만
    errors: ['rate<0.1'],
  },
};

const DOORIBANK_URL = __ENV.DOORIBANK_URL || 'http://localhost:8081';
const WOORIDOORI_URL = __ENV.WOORIDOORI_URL || 'http://localhost:8080';

// 테스트 시작 전에 실제 회원 데이터를 가져옵니다
let realMembers = [];

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
  
  const payload = JSON.stringify({
    id: email,
    password: password,
    name: member.name,
    phone: member.phone,
    birthDate: birthDate,
    birthBack: birthBack,
  });

  const params = {
    headers: { 'Content-Type': 'application/json' },
    tags: { name: 'SignupWithRealData' },
    timeout: '30s',
  };

  const res = http.post(`${WOORIDOORI_URL}/auth/join`, payload, params);

  const success = check(res, {
    '회원가입 성공': (r) => r.status === 200,
    '응답 시간 < 3초': (r) => r.timings.duration < 3000,
    '응답 본문 있음': (r) => r.body.length > 0,
  });

  if (!success) {
    errorRate.add(1);
    console.log(`회원가입 실패: ${member.name} (${email}), 상태: ${res.status}`);
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
  summary += '우리두리 스트레스 테스트 결과 (실제 DooriBank 회원 데이터 사용)\n';
  summary += '='.repeat(60) + '\n';
  summary += `총 요청 수: ${data.metrics.http_reqs.values.count}\n`;
  summary += `성공률: ${((1 - data.metrics.http_req_failed.values.rate) * 100).toFixed(2)}%\n`;
  summary += `평균 응답 시간: ${data.metrics.http_req_duration.values.avg.toFixed(2)}ms\n`;
  summary += `최대 응답 시간: ${data.metrics.http_req_duration.values.max.toFixed(2)}ms\n`;
  summary += `95% 응답 시간: ${data.metrics.http_req_duration.values['p(95)'].toFixed(2)}ms\n`;
  summary += `에러율: ${(data.metrics.http_req_failed.values.rate * 100).toFixed(2)}%\n`;
  summary += '='.repeat(60) + '\n';
  return summary;
}

