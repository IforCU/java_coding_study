package kr.banklearning.exercise;

import java.util.List;
import java.util.Map;

public interface HistoryMapper {
    List<Map<String, Object>> search(Map<String, Object> parameters);
}
