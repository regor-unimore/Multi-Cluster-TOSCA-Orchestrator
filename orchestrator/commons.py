import uuid


def create_uuid() -> str:
    return str(uuid.uuid4())


def is_valid_uuid(val:str) -> bool:
    try:
        uuid.UUID(val)
        return True

    except ValueError:
        return False