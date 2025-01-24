## BANK 서비스

종료된 [팀 프로젝트](https://github.com/sungnyuung/SN-BANK) 개선...
- 결제, 이체 기능 제공
- 원화, 외화 계좌 제공
- 원화 <-> 외화 간 거래에 실시간 환율 데이터 사용 중

## TODO

트랜잭션 분리
- [X] async 기반 트랜잭션 분리
- [ ] kafka 기반 트랜잭션 분리
- [X] Lexicographic Locking
- [ ] DB 락 vs Redisson

비동기 Open API 설계
- [X] CompletableFuture 기반 환율 Open API
- [ ] Timeout 동적 설정
- [ ] Spin Lock VS Sleep 성능 테스트
- [ ] 분산 환경 고려 (현재 딘일 서버 기준으로 구현됨)
- [ ] 테스트 코드 Timeout 설정 (외부 API 오류 시 테스트 코드에서 timeout 발생)
 
데이터 보안
- [X] AES 256 암호화
- [ ] SSL 전환

CI/CD 파이프라인
- [ ] Docker Layer 캐싱
- [X] AWS ECR
