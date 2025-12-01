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
  setupTimeout: '5m',
  stages: [
    { duration: '2m', target: 50 },
    { duration: '5m', target: 100 },
    { duration: '5m', target: 200 },
    { duration: '5m', target: 300 },
    { duration: '5m', target: 0 },
  ],
  thresholds: {
    http_req_duration: ['p(95)<10000'],
    http_req_failed: ['rate<0.30'],
    errors: ['rate<0.30'],
  },
};

const DOORIBANK_URL = "http://113.198.66.77:18170";
const WOORIDOORI_URL = "http://172.16.1.120:8080";

// 제외할 ID 목록을 상수로 선언
const EXCLUDED_IDS = [28, 29, 30, 31, 32, 33, 34];

export function setup() {
  console.log('=== DooriBank에서 실제 회원 데이터 가져오기 ===');
  console.log(`DooriBank URL: ${DOORIBANK_URL}`);
  
  const url = `${DOORIBANK_URL}/api/test/members`;
  console.log(`회원 데이터 조회 URL: ${url}`);
  
  let response;
  try {
    response = http.get(url, {
      timeout: '120s',
      tags: { name: 'Setup_GetMembers' },
    });
    
    console.log(`응답 상태: ${response.status}`);
    console.log(`응답 본문 길이: ${response.body ? response.body.length : 0} bytes`);
  } catch (e) {
    console.error(`HTTP 요청 실패: ${e.message}`);
    return { members: [] };
  }
  
  if (!response || response.status !== 200) {
    console.error(`회원 데이터 조회 실패: ${response ? response.status : 'no response'}`);
    return { members: [] };
  }
  
  if (!response.body || response.body.length === 0) {
    console.error(`응답 본문이 비어있습니다.`);
    return { members: [] };
  }
  
  let bodyText = response.body.trim();
  if (bodyText.charCodeAt(0) === 0xFEFF) {
    bodyText = bodyText.slice(1);
  }
  
  try {
    let parsedData = JSON.parse(bodyText);
    let members = null;
    
    if (Array.isArray(parsedData)) {
      members = parsedData;
    } else if (typeof parsedData === 'object' && parsedData !== null) {
      if (Array.isArray(parsedData.data)) {
        members = parsedData.data;
      } else if (Array.isArray(parsedData.members)) {
        members = parsedData.members;
      } else if (Array.isArray(parsedData.result)) {
        members = parsedData.result;
      } else if (Array.isArray(parsedData.resultData)) {
        members = parsedData.resultData;
      }
    }
    
    if (!members || !Array.isArray(members) || members.length === 0) {
      console.error(`회원 데이터를 찾을 수 없습니다.`);
      return { members: [] };
    }
    
    console.log(`=== ${members.length}명의 회원 데이터 로드 완료 ===`);
    
    // ID 28~34 제외 (setup 단계에서 미리 필터링)
    const filteredMembers = members.filter(m => {
      if (m.id !== undefined && m.id !== null) {
        const isExcluded = EXCLUDED_IDS.includes(m.id);
        if (isExcluded) {
          console.log(`제외된 회원: ID=${m.id}, 이름=${m.name}`);
        }
        return !isExcluded;
      }
      return true;
    });
    
    console.log(`ID 28~34 제외 후: ${filteredMembers.length}명`);
    
    // 계좌 정보가 있는 회원만 필터링
    const membersWithAccount = filteredMembers.filter(m => m.accountNumber && m.accountPassword);
    console.log(`계좌 정보가 있는 회원: ${membersWithAccount.length}명`);
    
    if (membersWithAccount.length > 0) {
      return { members: membersWithAccount };
    } else {
      console.warn(`⚠️ 계좌 정보가 있는 회원이 없습니다.`);
      return { members: filteredMembers };
    }
  } catch (e) {
    console.error(`회원 데이터 파싱 실패: ${e.message}`);
    return { members: [] };
  }
}

function getCardInfoForMember(member) {
  if (!member || !member.name) {
    console.log(`[카드정보] 회원 정보 누락`);
    return [];
  }

  // 두리뱅킹에서 회원의 모든 계좌를 조회 (계좌 = 카드로 사용)
  const query = `memberName=${encodeURIComponent(member.name)}`;
  const url = `${DOORIBANK_URL}/api/test/member-accounts?${query}`;

  try {
    const response = http.get(url, {
      timeout: '30s',
      tags: { name: 'FetchCardInfo' },
    });

    console.log(`[카드정보] ${member.name} - 응답 상태: ${response.status}`);

    if (response.status !== 200) {
      console.log(`[카드정보] 조회 실패 - status: ${response.status}, body: ${response.body ? response.body.substring(0, 300) : 'null'}`);
      return [];
    }

    const accounts = JSON.parse(response.body);
    
    if (Array.isArray(accounts)) {
      console.log(`[카드정보] ${member.name} - ✅ ${accounts.length}개 계좌 조회 성공`);
      if (accounts.length > 0) {
        console.log(`[카드정보] ${member.name} - 계좌번호: ${accounts.map(a => a.accountNumber).join(', ')}`);
      }
      
      // 계좌 정보를 카드 정보 형식으로 변환
      return accounts.map((account, idx) => ({
        cardNum: account.accountNumber,
        cardPw: account.accountPassword,
        expiryMmYy: '1229', // 테스트용 유효기간
        cardUserRegistNum: member.memberRegistNum ? member.memberRegistNum.substring(0, 6) : '000000',
        cardUserRegistBack: member.memberRegistNum ? member.memberRegistNum.substring(6, 7) : '1',
        cardCvc: '123', // 테스트용 CVC
        cardAlias: `${member.name}_계좌_${idx + 1}`,
      }));
    } else {
      console.log(`[카드정보] ${member.name} - ❌ 예상치 못한 응답 형식`);
      return [];
    }
  } catch (e) {
    console.log(`[카드정보] 요청 중 오류 - ${e.message}`);
    return [];
  }
}

export default function (data) {
  if (!data.members || data.members.length === 0) {
    console.log('실제 회원 데이터가 없습니다. 테스트를 건너뜁니다.');
    return;
  }
  
  const uniqueIndex = (__VU - 1) * 1000 + __ITER;
  const memberIndex = uniqueIndex % data.members.length;
  const member = data.members[memberIndex];
  
  // 이미 setup에서 필터링되었지만 이중 체크
  if (member.id !== undefined && member.id !== null && EXCLUDED_IDS.includes(member.id)) {
    console.log(`❌ ID ${member.id} 회원은 테스트에서 제외됩니다: ${member.name}`);
    return;
  }
  
  const memberName = member.name;
  const timestamp = Date.now();
  const randomSuffix = Math.random().toString(36).substring(7);
  const nameHash = member.name ? Array.from(member.name).map(c => c.charCodeAt(0).toString(36)).join('').substring(0, 5) : 'user';
  const email = `user${timestamp}_${randomSuffix}_${nameHash}@loadtest.com`;
  
  let birthDate = null;
  let birthBack = null;
  if (member.memberRegistNum && member.memberRegistNum.length === 7) {
    birthDate = member.memberRegistNum.substring(0, 6);
    birthBack = member.memberRegistNum.substring(6, 7);
  } else {
    birthDate = `9${Math.floor(Math.random() * 10)}${String(Math.floor(Math.random() * 12) + 1).padStart(2, '0')}${String(Math.floor(Math.random() * 28) + 1).padStart(2, '0')}`;
    birthBack = String(Math.floor(Math.random() * 4) + 1);
  }
  
  let phone = member.phone || '';
  phone = phone.replace(/[^0-9]/g, '');
  
  if (!member.name || !phone || !birthDate || !birthBack) {
    console.error(`필수 필드 누락: name=${member.name}, phone=${phone}`);
    errorRate.add(1);
    return;
  }
  
  const password = 'Test1234!';
  
  // ========== 1단계: 회원가입 ==========
  const signupPayload = JSON.stringify({
    id: email,
    password: password,
    name: memberName,
    phone: phone,
    birthDate: birthDate,
    birthBack: birthBack,
  });
  
  console.log(`✅ 회원가입 요청: ${memberName} (VU: ${__VU}, Index: ${memberIndex})`);

  const signupRes = http.post(
    `${WOORIDOORI_URL}/auth/join`,
    signupPayload,
    {
      headers: { 'Content-Type': 'application/json' },
      timeout: '60s',
      tags: { name: 'Signup' },
    }
  );

  const signupSuccess = check(signupRes, {
    '회원가입 성공': (r) => r.status === 200 || r.status === 201,
  });

  if (signupSuccess) {
    console.log(`✅ 회원가입 성공: ${memberName} (${email})`);
  } else {
    errorRate.add(1);
    console.log(`❌ 회원가입 실패: ${memberName}, 상태: ${signupRes.status}`);
    if (signupRes.status === 400) {
      return;
    }
  }

  sleep(1);

  // ========== 2단계: 로그인 ==========
  const loginPayload = JSON.stringify({
    id: email,
    password: password,
  });

  const loginRes = http.post(
    `${WOORIDOORI_URL}/auth/login`,
    loginPayload,
    {
      headers: { 'Content-Type': 'application/json' },
      timeout: '60s',
      tags: { name: 'Login' },
    }
  );

  const loginSuccess = check(loginRes, {
    '로그인 성공': (r) => r.status === 200,
  });

  if (!loginSuccess) {
    errorRate.add(1);
    console.log(`❌ 로그인 실패: ${email}, 상태: ${loginRes.status}`);
    return;
  }

  let accessToken = null;
  try {
    const loginData = JSON.parse(loginRes.body);
    accessToken = loginData.resultData?.tokens?.accessToken;
  } catch (e) {
    errorRate.add(1);
    console.log(`❌ 로그인 응답 파싱 실패: ${e.message}`);
    return;
  }

  if (!accessToken) {
    errorRate.add(1);
    console.log(`❌ 액세스 토큰 없음: ${email}`);
    return;
  }

  console.log(`✅ 로그인 성공: ${email}`);
  sleep(1);

  // ========== 3단계: 카드 등록 (모든 계좌를 카드로 등록) ==========
  const cardInfos = getCardInfoForMember(member);

  if (!cardInfos || cardInfos.length === 0) {
    console.log(`⚠️ 카드 등록 스킵: ${email} - 계좌 데이터 없음`);
    sleep(1);
  } else {
    console.log(`💳 카드 등록 시작: ${email}, 총 ${cardInfos.length}개 카드`);
    console.log(`💳 카드 목록: ${cardInfos.map(c => c.cardNum).join(', ')}`);
    let registeredCount = 0;

    for (let idx = 0; idx < cardInfos.length; idx++) {
      const cardInfo = cardInfos[idx];
      
      const cardPayloadObj = {
        cardNum: cardInfo.cardNum,
        cardPw: cardInfo.cardPw,
        expiryMmYy: cardInfo.expiryMmYy,
        cardUserRegistNum: cardInfo.cardUserRegistNum,
        cardUserRegistBack: cardInfo.cardUserRegistBack,
        cardCvc: cardInfo.cardCvc,
        cardAlias: cardInfo.cardAlias, // 이미 변환 함수에서 설정됨
      };

      const cardPayload = JSON.stringify(cardPayloadObj);
      console.log(`💳 [${idx + 1}/${cardInfos.length}] 카드번호: ${cardInfo.cardNum}, 별칭: ${cardPayloadObj.cardAlias}`);

      const cardRes = http.patch(
        `${WOORIDOORI_URL}/test/card/putCard/no-cvc`,
        cardPayload,
        {
          headers: {
            'Content-Type': 'application/json',
            Authorization: `Bearer ${accessToken}`,
          },
          timeout: '60s',
          tags: { name: 'CardRegistration' },
        }
      );

      console.log(`💳 [${idx + 1}/${cardInfos.length}] 응답 상태: ${cardRes.status}`);
      if (cardRes.body) {
        console.log(`💳 [${idx + 1}/${cardInfos.length}] 응답 본문: ${cardRes.body.substring(0, 300)}`);
      }

      const cardSuccess = check(cardRes, {
        '카드 등록 성공': (r) => r.status === 200,
      });

      if (cardSuccess) {
        registeredCount++;
        console.log(`✅ 카드 등록 성공 (${idx + 1}/${cardInfos.length}): ${cardPayloadObj.cardAlias}`);
      } else {
        errorRate.add(1);
        console.log(`❌ 카드 등록 실패 (${idx + 1}/${cardInfos.length}): 상태 ${cardRes.status}`);
      }

      // 각 카드 등록 사이에 충분한 딜레이 (DB 처리 시간 확보)
      sleep(1);
    }

    console.log(`💳 카드 등록 완료: ${email} - ${registeredCount}/${cardInfos.length}개 성공`);
    
    // 모든 카드 등록 후 실제로 등록된 카드 수 확인
    const verifyRes = http.get(
      `${WOORIDOORI_URL}/card`,
      {
        headers: {
          Authorization: `Bearer ${accessToken}`,
        },
        timeout: '30s',
        tags: { name: 'VerifyCards' },
      }
    );
    
    if (verifyRes.status === 200) {
      try {
        const verifyData = JSON.parse(verifyRes.body);
        const actualCardCount = verifyData.resultData ? verifyData.resultData.length : 0;
        console.log(`🔍 등록 검증: ${email} - 시도 ${cardInfos.length}개, 성공 응답 ${registeredCount}개, 실제 등록 ${actualCardCount}개`);
        
        if (actualCardCount !== cardInfos.length) {
          console.warn(`⚠️ 불일치 발견! ${email} - 예상 ${cardInfos.length}개 != 실제 ${actualCardCount}개`);
        }
      } catch (e) {
        console.log(`⚠️ 카드 등록 검증 실패: ${e.message}`);
      }
    }
    
    sleep(0.5);
  }

  // ========== 4단계: 목표 설정 ==========
  const today = new Date();
  const currentMonth = today.getMonth() + 1;
  const currentYear = today.getFullYear();
  const goalStartDate = `${currentYear}-${String(currentMonth).padStart(2, '0')}-01`;

  const goalPayloadObj = {
    goalJob: 'SALARY',
    goalStartDate: goalStartDate,
    goalIncome: '3000',
    previousGoalMoney: 200,
    essentialCategories: [],
  };
  
  const goalPayload = JSON.stringify(goalPayloadObj);

  const goalRes = http.put(
    `${WOORIDOORI_URL}/goal/setgoal`,
    goalPayload,
    {
      headers: {
        'Content-Type': 'application/json',
        Authorization: `Bearer ${accessToken}`,
      },
      timeout: '60s',
      tags: { name: 'SetGoal' },
    }
  );

  const goalSuccess = check(goalRes, {
    '목표 설정 성공': (r) => r.status === 200,
  });

  if (goalSuccess) {
    console.log(`✅ 목표 설정 성공: ${email}`);
  } else {
    errorRate.add(1);
    console.log(`❌ 목표 설정 실패: ${email}, 상태: ${goalRes.status}`);
  }

  sleep(1);

  // ========== 5단계: 목표 점수 계산 ==========
  const shouldTriggerBatch = (__VU === 1 && __ITER === 0);
  if (shouldTriggerBatch) {
    const calculateRes = http.get(
      `${WOORIDOORI_URL}/test/goal-score/calculate`,
      {
        headers: {
          Authorization: `Bearer ${accessToken}`,
        },
        timeout: '120s',
        tags: { name: 'CalculateGoalScore' },
      }
    );

    const calculateSuccess = check(calculateRes, {
      '점수 계산 성공': (r) => r.status === 200,
    });

    if (calculateSuccess) {
      console.log(`✅ 목표 점수 계산 성공`);
    } else {
      errorRate.add(1);
      console.log(`❌ 목표 점수 계산 실패: 상태 ${calculateRes.status}`);
    }

    sleep(2);
  }

  // ========== 6단계: 소비 리포트 조회 ==========
  const dashboardRes = http.get(
    `${WOORIDOORI_URL}/goal/report`,
    {
      headers: {
        Authorization: `Bearer ${accessToken}`,
      },
      timeout: '60s',
      tags: { name: 'ReportCheck' },
    }
  );

  const reportSuccess = check(dashboardRes, {
    '리포트 조회 성공': (r) => r.status === 200,
  });

  if (reportSuccess) {
    console.log(`✅ 리포트 조회 성공: ${email}`);
  } else {
    errorRate.add(1);
    console.log(`❌ 리포트 조회 실패: ${email}, 상태: ${dashboardRes.status}`);
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
  summary += '='.repeat(60) + '\n';
  summary += `총 요청 수: ${data.metrics.http_reqs.values.count}\n`;
  summary += `성공률: ${((1 - data.metrics.http_req_failed.values.rate) * 100).toFixed(2)}%\n`;
  summary += `평균 응답 시간: ${data.metrics.http_req_duration.values.avg.toFixed(2)}ms\n`;
  summary += `최대 응답 시간: ${data.metrics.http_req_duration.values.max.toFixed(2)}ms\n`;
  summary += `95% 응답 시간: ${data.metrics.http_req_duration.values['p(95)'].toFixed(2)}ms\n`;
  summary += `에러율: ${(data.metrics.http_req_failed.values.rate * 100).toFixed(2)}%\n`;
  summary += '\n';
  summary += '단계별 통계:\n';
  
  const tags = ['Signup', 'Login', 'CardRegistration', 'SetGoal', 'CalculateGoalScore', 'ReportCheck'];
  tags.forEach(tag => {
    const tagData = data.metrics.http_req_duration.values.tags?.[tag];
    if (tagData) {
      summary += `  ${tag}: 평균 ${tagData.avg.toFixed(2)}ms, 최대 ${tagData.max.toFixed(2)}ms\n`;
    }
  });
  
  summary += '='.repeat(60) + '\n';
  return summary;
}