微服务的整体业务需求
乘客和司机用户的管理：用户可以注册成为乘客或司机，注册时将存储用户的基本信息，并将其存入数据库。
用户身份验证：已注册的用户可以登录系统，通过用户名和密码进行身份验证。
安全性保障：使用 JWT 进行身份验证，在每次请求时进行令牌的校验和解析。
用户信息操作：允许用户查看和更新自己的个人信息。
错误处理和异常管理：确保业务流程中出现异常时给予合理的响应。
用户注册业务逻辑
用户注册是该微服务的核心功能之一，主要涉及以下操作：
检查用户是否已经存在。
对用户密码进行加密。
保存用户的基本信息到数据库中。
具体流程：
接收用户注册请求： 控制器（UserController）接收用户注册请求，传递一个 UserRegistrationDto 对象作为输入。
服务层处理逻辑：在 UserService 接口中定义了 registerUser(UserRegistrationDto) 方法。实现类 UserServiceImpl 中实现了 registerUser() 方法。
检查用户名是否存在：通过调用 UserRepository.existsByUsername(String username) 检查数据库中是否已经存在该用户。
加密用户密码：使用 PasswordEncoder 对用户输入的密码进行加密（通过 passwordEncoder.encode() 方法）。
保存用户信息：使用 UserRepository.save(User user) 将用户信息存入数据库。
返回注册成功消息或抛出异常：如果用户名已经存在，抛出 UserAlreadyExistsException。
如果注册成功，返回成功消息。




















用户登录与身份验证业务逻辑
用户登录涉及对用户提供的凭证（用户名和密码）进行身份验证，并返回一个 JWT 令牌供后续请求使用。
具体流程：
接收用户登录请求：
控制器（UserController）接收用户登录请求，传递一个 LoginRequestDto 对象。
服务层处理逻辑：
在 UserService 接口中定义了 loginUser(LoginRequestDto) 方法。实现类 UserServiceImpl 中实现了 loginUser() 方法。
验证用户密码：使用 UserRepository.findByUsername() 查找用户。使用 passwordEncoder.matches() 方法验证输入的密码是否与存储的加密密码匹配。
生成 JWT 令牌：如果验证成功，使用 JwtTokenProvider.generateToken() 生成 JWT 令牌。
返回 JWT 令牌：返回 JWT 令牌，供后续请求中验证用户身份使用。


JWT 身份验证逻辑
JWT 身份验证是保护后端 API 安全性的关键。通过每个请求附带的 JWT 令牌来验证用户的身份，并确保只有授权用户才能访问受保护的资源。
具体流程：
在每个请求中携带 JWT：客户端在 HTTP 头中携带 Authorization: Bearer <token> 来访问受保护的 API。
解析 JWT：使用 JwtTokenProvider.getUsernameFromJWT(token) 解析 JWT 并获取其中的用户信息。
验证 JWT：使用 JwtTokenProvider.validateToken(token) 验证令牌的有效性。如果 JWT 过期或者无效，则抛出 JwtException。
将用户身份添加到 Spring Security 上下文：使用 SecurityContextHolder 将经过验证的用户添加到当前的安全上下文，确保用户的身份被正确管理。




用户信息获取与更新
用户可以查看和更新个人信息。该业务逻辑包括：
获取用户资料：基于身份验证获取当前登录用户的详细信息。
更新用户资料：允许用户更新其个人资料，并将其保存到数据库。
具体流程：
获取用户信息：用户请求个人信息，通过 UserService.getUserProfile(String username) 方法获取。
更新用户信息：用户提交新的个人信息，通过 UserService.updateUserProfile(UserDto) 方法更新数据库中的用户信息。


整体架构及实现总结
控制器层：处理 HTTP 请求并与服务层进行交互。控制器根据用户的请求类型（例如注册、登录、更新信息）调用服务层的相应方法。
服务层：实现核心业务逻辑，例如用户注册、登录、更新等。服务层通过调用仓库层与数据库进行交互，并使用 JWT 提供身份验证和令牌生成的支持。
安全性层：JWT 提供令牌的生成、验证和解析。通过 Spring Security 将 JWT 的验证与整个系统的安全性挂钩。
数据访问层（仓库层）：实现与数据库的交互，负责用户数据的持久化存储和查询操作。