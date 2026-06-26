ALTER TABLE gen_table ADD (data_name VARCHAR2(200) DEFAULT '');

COMMENT ON COLUMN gen_table.data_name IS '数据源名称';

UPDATE sys_menu SET path = 'powerjob', component = 'monitor/powerjob/index', perms = 'monitor:powerjob:list', remark = 'powerjob控制台菜单' WHERE menu_id = 120;

-- ----------------------------
-- 第三方平台授权表
-- ----------------------------
create table sys_client (
    id                  number(20)    not null,
    client_id           varchar2(64)   default null,
    client_key          varchar2(32)   default null,
    client_secret       varchar2(255)  default null,
    grant_type          varchar2(255)  default null,
    device_type         varchar2(32)   default null,
    active_timeout      number(11)    default 1800,
    timeout             number(11)    default 604800,
    status              char(1)       default '0',
    del_flag            char(1)       default '0',
    create_dept         number(20)    default null,
    create_by           number(20)    default null,
    create_time         date,
    update_by           number(20)    default null,
    update_time         date
);

alter table sys_client add constraint pk_sys_client primary key (id);

comment on table sys_client                         is '系统授权表';
comment on column sys_client.id                     is '主键';
comment on column sys_client.client_id              is '客户端id';
comment on column sys_client.client_key             is '客户端key';
comment on column sys_client.client_secret          is '客户端秘钥';
comment on column sys_client.grant_type             is '授权类型';
comment on column sys_client.device_type            is '设备类型';
comment on column sys_client.active_timeout         is 'token活跃超时时间';
comment on column sys_client.timeout                is 'token固定超时';
comment on column sys_client.status                 is '状态（0正常 1停用）';
comment on column sys_client.del_flag               is '删除标志（0代表存在 2代表删除）';
comment on column sys_client.create_dept            is '创建部门';
comment on column sys_client.create_by              is '创建者';
comment on column sys_client.create_time            is '创建时间';
comment on column sys_client.update_by              is '更新者';
comment on column sys_client.update_time            is '更新时间';

insert into sys_client values (1, 'e5cd7e4891bf95d1d19206ce24a7b32e', 'pc', 'pc123', 'password', 'pc', 1800, 604800, 0, 0, 103, 1, sysdate, 1, sysdate);
insert into sys_client values (2, '428a8310cd442757ae699df5d894f051', 'app', 'app123', 'password,sms', 'android', 1800, 604800, 0, 0, 103, 1, sysdate, 1, sysdate);

insert into sys_dict_type values(11, '000000', '授权类型', 'sys_grant_type',     '0', 103, 1, sysdate, null, null, '认证授权类型');
insert into sys_dict_type values(12, '000000', '设备类型', 'sys_device_type',    '0', 103, 1, sysdate, null, null, '客户端设备类型');

insert into sys_dict_data values(30, '000000', 0,  '密码认证', 'password',   'sys_grant_type',   '',   'default', 'N', '0', 103, 1, sysdate, null, null, '密码认证');
insert into sys_dict_data values(31, '000000', 0,  '短信认证', 'sms',        'sys_grant_type',   '',   'default', 'N', '0', 103, 1, sysdate, null, null, '短信认证');
insert into sys_dict_data values(32, '000000', 0,  '邮件认证', 'email',      'sys_grant_type',   '',   'default', 'N', '0', 103, 1, sysdate, null, null, '邮件认证');
insert into sys_dict_data values(33, '000000', 0,  '小程序认证', 'xcx',      'sys_grant_type',   '',   'default', 'N', '0', 103, 1, sysdate, null, null, '小程序认证');
insert into sys_dict_data values(35, '000000', 0,  'PC',      'pc',          'sys_device_type',  '',   'default', 'N', '0', 103, 1, sysdate, null, null, 'PC');
insert into sys_dict_data values(36, '000000', 0,  '安卓',     'android',    'sys_device_type',  '',   'default', 'N', '0', 103, 1, sysdate, null, null, '安卓');
insert into sys_dict_data values(37, '000000', 0,  'iOS',     'ios',         'sys_device_type',  '',   'default', 'N', '0', 103, 1, sysdate, null, null, 'iOS');
insert into sys_dict_data values(38, '000000', 0,  '小程序',     'xcx',      'sys_device_type',  '',   'default', 'N', '0', 103, 1, sysdate, null, null, '小程序');

-- 二级菜单
insert into sys_menu values('123',  '客户端管理',   '1',   '11', 'client',           'system/client/index',          '', 1, 0, 'C', '0', '0', 'system:client:list',          'international', 103, 1, sysdate, null, null, '客户端管理菜单');
-- 客户端管理按钮
insert into sys_menu values('1061', '客户端管理查询', '123', '1',  '#', '', '', 1, 0, 'F', '0', '0', 'system:client:query',        '#', 103, 1, sysdate, null, null, '');
insert into sys_menu values('1062', '客户端管理新增', '123', '2',  '#', '', '', 1, 0, 'F', '0', '0', 'system:client:add',          '#', 103, 1, sysdate, null, null, '');
insert into sys_menu values('1063', '客户端管理修改', '123', '3',  '#', '', '', 1, 0, 'F', '0', '0', 'system:client:edit',         '#', 103, 1, sysdate, null, null, '');
insert into sys_menu values('1064', '客户端管理删除', '123', '4',  '#', '', '', 1, 0, 'F', '0', '0', 'system:client:remove',       '#', 103, 1, sysdate, null, null, '');
insert into sys_menu values('1065', '客户端管理导出', '123', '5',  '#', '', '', 1, 0, 'F', '0', '0', 'system:client:export',       '#', 103, 1, sysdate, null, null, '');

-- 角色菜单权限
insert into sys_role_menu values ('2', '1061');
insert into sys_role_menu values ('2', '1062');
insert into sys_role_menu values ('2', '1063');
insert into sys_role_menu values ('2', '1064');
insert into sys_role_menu values ('2', '1065');


update sys_dept set leader = null;
ALTER TABLE sys_dept MODIFY (leader NUMBER(20))
