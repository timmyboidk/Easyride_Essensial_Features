乘客下单
   * 乘客输入起点和终点，选择车型、服务类型和支付方式。
   * 系统预估费用、距离和行程时间。
   * 订单进入待匹配状态。
订单匹配
   * 系统自动匹配附近的空闲司机，发送接单请求。
   * 司机在规定时间内响应接单请求。
   * 如果超时未响应或被拒绝，继续匹配下一位司机。
订单状态管理
   * 订单状态流转：PENDING（待接单）→ ACCEPTED（已接单）→ ARRIVED（司机已到达）→ IN_PROGRESS（行程中）→ COMPLETED（已完成）→ CANCELLED（已取消）。
   * 乘客可在司机未接单前取消订单。
   * 司机可更新订单状态，通知乘客当前进度。
实时位置跟踪
   * 司机定期上传当前位置。
   * 乘客实时查看司机位置，获取预计到达时间（ETA）。
   * 行程结束后，支持查看行驶路线。
 乘客下单业务逻辑
流程概述：
1. 接收下单请求：
乘客通过客户端提交下单请求，包含起点、终点、车型、服务类型和支付方式等信息。
2. 验证乘客信息：
系统验证乘客是否存在，确保请求合法。
3. 预估费用和行程：
系统根据起点和终点，预估行程距离、时间和费用。
4. 创建订单：
生成新的订单，状态设置为 PENDING（待接单）。
将订单保存到数据库。
5. 返回订单详情：
将订单的详细信息返回给乘客，包括预估费用和行程信息。
涉及的类、接口和方法：
OrderController
   * createOrder(OrderRequestDto orderRequest)：处理乘客的下单请求。
OrderService 接口
   * createOrder(OrderRequestDto orderRequest)：定义下单业务逻辑的方法。
OrderServiceImpl 实现类
   * 实现了 createOrder 方法，包含下单的具体业务逻辑。
OrderRequestDto 和 OrderResponseDto
   * 数据传输对象，用于在客户端和服务器之间传递下单请求和响应数据。
Order 实体类
   * 表示订单的数据库实体，包含订单的详细信息。


验证乘客信息： 通过 passengerRepository 检查乘客是否存在。
预估费用和行程： 调用内部方法 estimateCost、estimateDistance、estimateDuration 计算预估值。
创建并保存订单： 将订单信息保存到数据库。
返回订单详情： 将订单转换为 OrderResponseDto 并返回。




订单匹配业务逻辑
流程概述：
1. 查找可用司机：
   * 系统查询附近的空闲司机列表。
2. 发送接单请求：
   * 向司机发送接单请求，司机可以在规定时间内响应。
3. 处理司机响应：
   * 如果司机接受订单，更新订单状态为 ACCEPTED，并将司机分配给订单。
   * 如果司机拒绝或超时未响应，继续匹配下一位司机。
4. 更新司机状态：
   * 将接受订单的司机状态更新为 不可用，防止重复分配。
涉及的类、接口和方法：
OrderController
   * acceptOrder(Long orderId, Long driverId)：处理司机接单的请求。
OrderService 接口
   * acceptOrder(Long orderId, Long driverId)：定义订单接单的业务逻辑方法。
OrderServiceImpl 实现类
   * 实现了 acceptOrder 方法，包含订单匹配的具体业务逻辑。
Order、Driver 实体类
   * Order 包含订单信息，需要更新状态和关联的司机。
   * Driver 包含司机信息，需要更新可用状态。


查找订单和司机： 通过 orderRepository 和 driverRepository 获取订单和司机信息。
检查司机可用性： 确保司机当前处于空闲状态。
更新订单状态： 将订单状态更新为 ACCEPTED，并关联司机。
更新司机状态： 将司机状态更新为 不可用。


________________


订单状态管理业务逻辑
流程概述：
1. 更新订单状态：
   * 订单状态可以由司机或系统更新，反映订单的当前进度。
2. 状态流转规则：
   * PENDING → ACCEPTED → ARRIVED → IN_PROGRESS → COMPLETED。
   * 乘客可以在 PENDING 状态取消订单，订单状态更新为 CANCELLED。
3. 释放司机资源：
   * 当订单状态更新为 COMPLETED 或 CANCELLED，需要将司机状态更新为 可用。
涉及的类、接口和方法：
OrderController
   * updateOrderStatus(Long orderId, String status)：处理订单状态更新的请求。
OrderService 接口
   * updateOrderStatus(Long orderId, String status)：定义订单状态更新的业务逻辑方法。
OrderServiceImpl 实现类
   * 实现了 updateOrderStatus 方法，包含订单状态管理的具体业务逻辑。
Order、Driver 实体类
   * 需要更新订单的状态字段和司机的可用状态。


* 查找订单： 获取需要更新的订单。
* 更新订单状态： 将订单状态更新为新的状态值。
* 释放司机资源： 如果订单已完成或取消，将司机状态更新为 可用。


实时位置跟踪业务逻辑
流程概述：
1. 司机上传当前位置：
   * 司机客户端定期向服务器发送位置信息。
2. 乘客查看司机位置：
   * 乘客客户端实时请求司机的位置，以获取预计到达时间（ETA）。
3. 位置数据的存储和传输：
   * 位置数据可以暂存于缓存或数据库，或通过 WebSocket 实时传输。
涉及的类、接口和方法：
* Location 实体类
   * 表示位置的经纬度信息。
* 位置服务（未详细实现）
   * 可以创建专门的服务来处理位置更新和获取。
实现说明：
* 由于实时位置跟踪涉及到实时通信和高频率的数据更新，这里可以考虑使用 WebSocket 或消息队列来实现。
* 在订单服务中，可以添加接口来接收和提供位置数据，或者与专门的定位服务进行集成。