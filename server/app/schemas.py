from pydantic import BaseModel, EmailStr, Field


class RegisterIn(BaseModel):
    name: str = Field(min_length=1, max_length=60)
    email: EmailStr
    password: str = Field(min_length=6, max_length=128)


class LoginIn(BaseModel):
    email: EmailStr
    password: str


class UpdateMeIn(BaseModel):
    displayName: str = Field(min_length=1, max_length=60)


class PresenceIn(BaseModel):
    online: bool


class DeviceIn(BaseModel):
    fcmToken: str = Field(default="", max_length=512)


class DirectChatIn(BaseModel):
    otherUid: str


class GroupChatIn(BaseModel):
    name: str = Field(min_length=1, max_length=80)
    memberUids: list[str] = Field(default_factory=list)


class SendMessageIn(BaseModel):
    text: str = Field(min_length=1, max_length=2000)
