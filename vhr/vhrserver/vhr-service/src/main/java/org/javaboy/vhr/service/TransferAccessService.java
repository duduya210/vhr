package org.javaboy.vhr.service;

import org.javaboy.vhr.mapper.MenuMapper;
import org.javaboy.vhr.model.Hr;
import org.javaboy.vhr.model.Menu;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import java.util.List;

/** Explicit menu check for the new APIs, including employee lookup endpoints. */
@Service
public class TransferAccessService {
    private final MenuMapper menuMapper;
    public TransferAccessService(MenuMapper menuMapper) { this.menuMapper = menuMapper; }
    public Hr requireMenu(String path) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || !(authentication.getPrincipal() instanceof Hr hr))
            throw new AccessDeniedException("请先登录");
        if (!contains(menuMapper.getMenusByHrId(hr.getId()), path))
            throw new AccessDeniedException("没有该功能的操作权限");
        return hr;
    }
    private boolean contains(List<Menu> menus, String path) {
        if (menus == null) return false;
        return menus.stream().filter(menu -> !Boolean.FALSE.equals(menu.getEnabled()))
                .anyMatch(menu -> path.equals(menu.getPath()) || contains(menu.getChildren(), path));
    }
}
