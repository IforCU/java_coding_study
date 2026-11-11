package kr.banklearning.servlet.dao;

/** SQL 원문과 연결 정보를 응답/로그에 노출하지 않는다. */
public final class DataAccessException extends RuntimeException {
    public DataAccessException() {
        super("데이터 저장소를 이용할 수 없습니다.");
    }
}
