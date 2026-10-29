package org.javaboy.vhr;

import org.javaboy.vhr.mapper.MenuMapper;
import org.javaboy.vhr.model.*;
import org.javaboy.vhr.service.TransferAccessService;
import org.junit.jupiter.api.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import java.util.List;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

class TransferAccessTests {
    @AfterEach void cleanup() { SecurityContextHolder.clearContext(); }
    @Test void menuPermissionsAreRequiredEvenForAuthenticatedUsers() {
        MenuMapper mapper = mock(MenuMapper.class);
        TransferAccessService access = new TransferAccessService(mapper);
        assertThrows(AccessDeniedException.class, () -> access.requireMenu("/per/mv"));
        Hr actor = new Hr(); actor.setId(7); actor.setName("操作员");
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(actor,null,List.of()));
        when(mapper.getMenusByHrId(7)).thenReturn(List.of());
        assertThrows(AccessDeniedException.class, () -> access.requireMenu("/per/mv"));
        Menu child = new Menu(); child.setPath("/per/mv"); child.setEnabled(true);
        Menu parent = new Menu(); parent.setChildren(List.of(child));
        when(mapper.getMenusByHrId(7)).thenReturn(List.of(parent));
        assertSame(actor, access.requireMenu("/per/mv"));
        assertThrows(AccessDeniedException.class, () -> access.requireMenu("/sys/log"));
        child.setEnabled(false);
        assertThrows(AccessDeniedException.class, () -> access.requireMenu("/per/mv"));
    }
}
