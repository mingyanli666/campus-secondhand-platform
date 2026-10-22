package com.digital.platform.controller;

import com.digital.platform.common.enums.RoleType;
import com.digital.platform.common.result.Result;
import com.digital.platform.dto.CommentDTO;
import com.digital.platform.security.LoginUser;
import com.digital.platform.security.PublicApi;
import com.digital.platform.security.UserContext;
import com.digital.platform.service.CommentService;
import com.digital.platform.vo.CommentVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "12-留言咨询", description = "发布留言、回复、查看留言列表")
@RestController
@RequestMapping("/api/comment")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    /**
     * 留言列表是公开的（游客也能看别人怎么问的），所以标 @PublicApi。
     * 但登录后要多算一个 canDelete 字段，让前端知道哪些留言自己能删。
     */
    @PublicApi
    @Operation(summary = "查询商品的留言列表（公开）")
    @GetMapping("/list")
    public Result<List<CommentVO>> list(@RequestParam Long productId) {
        LoginUser user = UserContext.get();
        Long userId = user == null ? null : user.getUserId();
        boolean isAdmin = user != null && RoleType.isAdmin(user.getRole());
        return Result.success(commentService.listByProduct(productId, userId, isAdmin));
    }

    @Operation(summary = "发布留言或回复（不传 parentId 即为一级留言）")
    @PostMapping
    public Result<Long> create(@Valid @RequestBody CommentDTO dto) {
        LoginUser user = UserContext.require();
        Long id = commentService.create(user.getUserId(), user.getNickname(), dto);
        return Result.success(dto.getParentId() == null ? "留言成功" : "回复成功", id);
    }

    @Operation(summary = "删除留言")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        LoginUser user = UserContext.require();
        commentService.delete(user.getUserId(), RoleType.isAdmin(user.getRole()), id);
        return Result.success("已删除", null);
    }
}
