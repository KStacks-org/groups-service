package com.kaustack.groups.controller;

import com.kaustack.groups.dto.request.*;
import com.kaustack.groups.dto.response.*;
import com.kaustack.groups.model.Group;
import com.kaustack.groups.service.GroupService;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import lombok.RequiredArgsConstructor;

import jakarta.validation.Valid;

import java.util.List;


@RestController
@RequiredArgsConstructor
public class GroupController {
    private final GroupService groupService;

    @PostMapping("/")
    public ResponseEntity<AddGroupResponse> addGroup(
            @Valid @RequestBody AddGroupRequest addGroupRequest
    ){
       Group group = groupService.addGroup(addGroupRequest);
       AddGroupResponse response = AddGroupResponse.from(group);
       return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/")
    public ResponseEntity<GetGroupsResponse> getGroups(
            @Valid @RequestBody GetGroupsRequest request
    ) {
        List<Group> groups = groupService.getGroups(request);
        GetGroupsResponse response = GetGroupsResponse.from(groups);
        return ResponseEntity.ok(response);
    }
}
