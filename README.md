## 💰 Banking 서비스

종료된 [팀 프로젝트](https://github.com/sungnyuung/SN-BANK) 디벨롭하기...🔥

### 기능
- [X] 이체 기능
- [X] 원화 및 외화 계좌 제공 (원화, 외화 간 거래에 실시간 환율 데이터 사용)
- [X] 결제 관련 Open API 제공
- [ ] 무중단 데이터 정합성 체크 기능
- [ ] 실시간 여러 환율 페이지 제공

<br>

## 📚 List of Refactoring Tasks

### IDOR (Insecure Direct Object Reference) 해결
![](/img/transfer-table-design.png)
- 이슈 발생: 상대방 계좌 & 이체 내역 접근 가능
- 해결 방법: Account 접근 제한 설정 및 *테이블 설계 변경해 상대 이체 내역 접근 차단*
- 기대: 민감한 정보 보호
- [x] [Account & User 엔티티 접근 제한](https://github.com/imzero238/Banking-API/blob/develop/src/main/java/banking/account/service/AccountService.java#L130) (거래 당사자는 상대 정보 일부 접근 가능)
- [x] 공개용 xxPublicInfo DTO 생성 (민감한 정보 제외하고 반환)

<br>

### 트랜잭션 분리
![](/img/transaction-design.png)
- 이슈 발생: 한 트랜잭션에서 많은 락 점유 -> 데드락 및 응답 지연 발생
- 해결 방법: 트랜잭션 분리해 락 점유율 감소
- 기대: 데드락 해결 및 NIO 스레드 응답 속도 개선
- [X] [@Async 트랜잭션 분리](https://github.com/imzero238/Banking-API/commit/394ffeaf556e519ce6e51d426ed19f458405166e) (**트랜잭션 보장을 위해 CallerRunsPolicy** 설정 -> NIO 스레드 지연 발생, MQ 해결 예정)
- [ ] Kafka 기반 트랜잭션 분리 (Outbox Pattern, CDC로 분리된 트랜잭션 보장 예정)
- [X] [Ordered Locking](https://github.com/imzero238/Banking-API/blob/develop/src/main/java/banking/transfer/service/TransferService.java#L101) -> 데드락 해결

<br>

### CompletableFuture 기반 Open API 설계
![](/img/exchange-rate-design.png)
- 이슈 발생: 수많은 스레드의 상태 전환 문제 (Context Switching 비용)
- 해결 방법: Spin Lock, Sleep 등 여러 방법 중 CPU 사용률 낮고, RPS가 큰 방식 채택
- 기대: 효율적인 CPU 사용
- [X] [ReentrantLock으로 Open API 호출 제한](https://github.com/imzero238/Banking-API/blob/develop/src/main/java/banking/exchangeRate/ExchangeRateService.java#L39) -> 네트워크 비용 절감 & Forbidden 오류 코드 대비
- [X] CompletableFuture 기반 환율 Open API [설계](https://github.com/imzero238/exchange-rate-open-api-test?tab=readme-ov-file#%ED%99%98%EC%9C%A8-open-api-%EC%84%A4%EA%B3%84) (Lock 사용 최소화)
- [ ] Timeout 동적 설정
- [X] Spin Lock vs Sleep 방식 [CPU usage & RPS 모니터링](https://github.com/imzero238/exchange-rate-open-api-test?tab=readme-ov-file#cpu-usage--requests-per-second-%EB%AA%A8%EB%8B%88%ED%84%B0%EB%A7%81)
- [ ] 분산 환경 고려 (현재 딘일 서버 기준으로 구현됨)
- [ ] 테스트 코드 Timeout 설정 (외부 API 오류 시 테스트 코드에서 timeout 발생)

<br>

### 데이터 보안
- 이슈 발생: AES 256 알고리즘으로 암호화했지만, 암호화를 위해 여러 데이터를 여러 서비스가 공유해야 하는 문제점
- 해결 방법: SSL 전환
- [X] AES 256 암호화
- [ ] SSL 전환

<br>

### CI/CD 파이프라인
- 이슈 발생: 코드 증가로 CI 작업만 5m 23s 소요
- 해결 방법: CI/CD 파이프라인 개선
- 기대: 코드 증가에도 빠른 CI/CD 가능
- [X] Gradle 캐싱
- [ ] Docker Layer 캐싱
- [ ] 파이프라인 병렬화
- [ ] Docker Hub -> AWS ECR 전환

