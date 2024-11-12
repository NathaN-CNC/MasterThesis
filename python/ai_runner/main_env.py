from main import create_app, get_model
import os
# https://stackoverflow.com/questions/51025893/flask-at-first-run-do-not-use-the-development-server-in-a-production-environmen
import waitress


model_name = os.getenv('MODEL_NAME', None)
if model_name is None:
    raise ValueError('MODEL_NAME environment variable not set')


port = os.getenv('PORT', None)
if port is None:
    raise ValueError('PORT environment variable not set')

dev = os.getenv('DEV', True)

model = get_model(model_name)
print(f'Running API for model {model_name}')

app = create_app(model)
print(f'Serving on port {port}')


if dev:
    app.run(host='0.0.0.0', port=int(port))
else:
    waitress.serve(
        app, 
        host='0.0.0.0', port=int(port),
        threads=max(os.cpu_count(), 1))
