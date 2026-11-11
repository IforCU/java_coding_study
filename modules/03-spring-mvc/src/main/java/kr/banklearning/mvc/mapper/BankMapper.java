package kr.banklearning.mvc.mapper;

import java.time.OffsetDateTime;
import java.util.List;
import kr.banklearning.mvc.dto.BankViews.AccountView;
import kr.banklearning.mvc.dto.BankViews.CustomerView;
import kr.banklearning.mvc.dto.BankViews.TransactionView;
import org.apache.ibatis.annotations.Param;

public interface BankMapper {
    List<CustomerView> customers();

    CustomerView customer(@Param("id") String id);

    int insertCustomer(@Param("customer") CustomerView customer);

    int changeStatus(@Param("id") String id, @Param("status") String status);

    List<AccountView> accounts(@Param("customerId") String customerId);

    AccountView account(@Param("id") String id);

    int insertAccount(@Param("account") AccountView account);

    List<TransactionView> history(
            @Param("accountId") String accountId,
            @Param("fromInclusive") OffsetDateTime fromInclusive,
            @Param("toExclusive") OffsetDateTime toExclusive);

    boolean ready();
}
