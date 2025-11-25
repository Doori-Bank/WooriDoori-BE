// 버스트 공격 - 순간 폭탄 (실제 DooriBank 회원 데이터 사용)
import http from 'k6/http';
import { check } from 'k6';

export const options = {
  vus: 5000,        // 5000명 동시 가상 사용자
  duration: '30s',  // 30초 동안 유지
  thresholds: {
    http_req_duration: ['max<5000'],  // 최대 5초
  },
};

const DOORIBANK_URL = __ENV.DOORIBANK_URL || 'http://localhost:8081';
const WOORIDOORI_URL = __ENV.WOORIDOORI_URL || 'http://localhost:8080';

// 테스트 시작 전에 실제 회원 데이터를 가져옵니다
export function setup() {
  console.log('=== DooriBank에서 실제 회원 데이터 가져오기 ===');
  console.log(`DooriBank URL: ${DOORIBANK_URL}`);
  
  const url = `${DOORIBANK_URL}/api/test/members`;
  console.log(`회원 데이터 조회 URL: ${url}`);
  
  const response = http.get(url, {
    timeout: '30s',
    tags: { name: 'Setup_GetMembers' },
  });
  
  console.log(`응답 상태: ${response.status}`);
  
  if (response.status !== 200) {
    console.error(`회원 데이터 조회 실패: ${response.status}`);
    console.error(`응답 본문: ${response.body}`);
    console.error(`응답 헤더: ${JSON.stringify(response.headers)}`);
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
    timeout: '5s',
    tags: { name: 'BurstAttackWithRealData' },
  };

  const res = http.post(`${WOORIDOORI_URL}/auth/join`, payload, params);

  check(res, {
    '버스트 공격 성공': (r) => r.status === 200,
    '타임아웃 없음': (r) => r.status !== 0,
  });
}

