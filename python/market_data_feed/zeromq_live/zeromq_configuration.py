import zmq


class ZeroMqConfiguration:
    CONTEXT = None

    @staticmethod
    def get_context(*args, **kwargs) -> zmq.Context:
        if ZeroMqConfiguration.CONTEXT is None:
            ZeroMqConfiguration.CONTEXT = zmq.Context(*args, **kwargs)
        return ZeroMqConfiguration.CONTEXT

    def __init__(self, url: str, port: int, topic: str = None, protocol: str = "tcp"):
        self.url = url
        self.port = port
        self.topic = topic
        self.protocol = protocol
        self.ipc_address = None

    def get_address(self):
        if self.ipc_address is not None:
            return self.ipc_address

        url = rf"{self.protocol}://{self.url}:{self.port}"
        return url

    def get_server_address(self):
        if self.ipc_address is not None:
            return f"{self.ipc_address}/*"

        url = rf"{self.protocol}://*:{self.port}"

        return url

    def set_ipc(self, address: str):
        self.url = rf"{address}"
        self.protocol = "ipc"
        self.ipc_address = f"ipc:///{address}"

    def __str__(self) -> str:
        if self.ipc_address is not None:
            return self.ipc_address

        return rf"{self.protocol} {self.topic}@{self.url}:{self.port}"
