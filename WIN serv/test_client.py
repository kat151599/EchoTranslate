import argparse, json
import httpx
p=argparse.ArgumentParser(); p.add_argument("image"); p.add_argument("--url",default="http://127.0.0.1:8765"); p.add_argument("--key",default=""); p.add_argument("--session",default="test"); p.add_argument("--source",default="auto"); p.add_argument("--target",default="ru"); a=p.parse_args()
with open(a.image,"rb") as f:
    headers = {"Authorization": f"Bearer {a.key}"} if a.key else {}
    r=httpx.post(a.url.rstrip("/")+"/v1/screen/translate", headers=headers, files={"image":("screen.jpg",f,"image/jpeg")}, data={"source_lang":a.source,"target_lang":a.target,"session_id":a.session}, timeout=120)
print(r.status_code); print(json.dumps(r.json(),ensure_ascii=False,indent=2))
