import argparse
import joblib
import pandas as pd

features = [
    'rate',
    'sttl',
    'sload',
    'dload',
    'ct_srv_src',
    'ct_state_ttl',
    'ct_dst_ltm',
    'ct_src_dport_ltm',
    'ct_dst_sport_ltm',
    'ct_dst_src_ltm',
    'ct_src_ltm',
    'ct_srv_dst',
    'state_CON',
    'state_INT',
]


def test_model(model, data):
    df = pd.DataFrame([data], columns=features)
    result = model.predict_proba(df)[0]
    return result


def create_app(model):
    from flask import Flask, request, jsonify

    app = Flask(model.__class__.__name__)

    @app.route('/predict', methods=['POST'])
    def predict():
        data = request.json
        try:
            if not isinstance(data, list):
                raise ValueError('Input data must be a list')

            label = None
            if len(data) == len(features) + 1:
                label = data.pop()

            if len(data) != len(features):
                raise ValueError(
                    f'Input data must have {len(features)} elements, but has {len(data)} elements.')

            result = test_model(model, data)
            return jsonify(result.tolist())
        except ValueError as e:
            return jsonify({'error': str(e)})

    return app


def get_model(model_name):
    model = joblib.load(f'models/{model_name}.pkl')
    return model


def main():
    parser = argparse.ArgumentParser(description='Run AI model')
    parser.add_argument('model', type=str, help='Model name')
    parser.add_argument('--input', type=str,
                        help='Input string to be evaluated to an array')
    parser.add_argument('--api', action='store_true',
                        help='Run the script as an API')
    parser.add_argument('--port', type=int, default=5000,
                        help='Port to run the API')

    args = parser.parse_args()
    model = get_model(args.model)

    if args.api:
        print(f'Running API for model {args.model}')
        app = create_app(model)
        app.run(port=args.port)
    else:
        input_data = eval(args.input)
        if not isinstance(input_data, list):
            raise ValueError('Input data must be a list')

        if len(input_data) != len(features):
            raise ValueError(
                f'Input data must have {len(features)} elements, but has {len(input_data)} elements.')

        print(test_model(model, input_data))


if __name__ == "__main__":
    main()
