package kr.banklearning.mvc.config;

import static org.junit.jupiter.api.Assertions.*;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.atomic.AtomicInteger;
import kr.banklearning.mvc.mapper.BankMapper;
import kr.banklearning.mvc.service.BankService;
import kr.banklearning.mvc.web.BankController;
import org.junit.jupiter.api.Test;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.jdbc.datasource.AbstractDataSource;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.mock.web.MockServletContext;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;

class ContextWiringTest {
    @Test
    void xmlRootAndAnnotationChildWireMapperServiceTransactionAndControllerWithoutDatabase() {
        AtomicInteger connections = new AtomicInteger();
        AbstractDataSource dataSource =
                new AbstractDataSource() {
                    @Override
                    public Connection getConnection() throws SQLException {
                        connections.incrementAndGet();
                        throw new SQLException("Test has no DB");
                    }

                    @Override
                    public Connection getConnection(String username, String password)
                            throws SQLException {
                        return getConnection();
                    }
                };
        try (GenericApplicationContext root = new GenericApplicationContext()) {
            new XmlBeanDefinitionReader(root).loadBeanDefinitions("classpath:root-context.xml");
            root.removeBeanDefinition("dataSource");
            root.registerBeanDefinition(
                    "dataSource",
                    new RootBeanDefinition(AbstractDataSource.class, () -> dataSource));
            root.refresh();
            assertNotNull(root.getBean(BankMapper.class));
            assertTrue(AopUtils.isAopProxy(root.getBean(BankService.class)));
            assertInstanceOf(
                    DataSourceTransactionManager.class, root.getBean("transactionManager"));
            try (AnnotationConfigWebApplicationContext child =
                    new AnnotationConfigWebApplicationContext()) {
                child.setParent(root);
                child.setServletContext(new MockServletContext());
                child.register(WebConfig.class);
                child.refresh();
                assertNotNull(child.getBean(BankController.class));
                assertNotNull(child.getBean("requestMappingHandlerMapping"));
                assertFalse(child.containsLocalBean("bankService"));
            }
            assertEquals(0, connections.get(), "컨텍스트 검증을 실제 DB 통합 테스트로 집계하지 않는다.");
        }
    }
}
