## 响应成功

- cmd: 111111

## 响应失败

- cmd: 000000

## 发送消息给指定用户

```
cmd: "100001"
 - reciverId: '' //接口用户ID
 - senderId: '' //发送用户ID
 - content: '' //消息体
 - targetType：'' //消息类型：用户 user、群组 group
 - messageId: ''//消息ID，通过时间戳确定唯一性
 - messageType: ''//消息类型，文本 | 图片 | 文件 |语音
 - readStatus: '' //消息是否已读 read/rent
 
```

## 测试心跳

```
cmd: 100002

【响应参数】
cmd: 1100002
 - code || code === 200 ? 成功:失败
 - message 
```

## 查询用户信息

>查询用户信息，优先级为：先查询在线用户，然后按照最近登录时间排序

```
cmd: 100003


【响应参数】

cmd: 1100003
 - code
 - message
 - data []userInfoL
```

## 推送更新查询用户列表信息

>推送消息给前端，需要更新查询用户列表

```
cmd: 1100005
```

