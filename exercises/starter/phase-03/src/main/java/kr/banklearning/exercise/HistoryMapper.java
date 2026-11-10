package kr.banklearning.exercise;

import java.util.List;
import java.util.Map;

/** accountId는 필수이고 from/to는 선택 검색 파라미터이다. */
public interface HistoryMapper {
    List<Map<String, Object>> search(Map<String, Object> parameters);
}
