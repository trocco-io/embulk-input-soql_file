module Embulk
  module Input

    class Soql < InputPlugin
      require 'restforce'

      Plugin.register_input("soql", self)

      MAX_GUESS_RECORDS_NUM = 30

      def self.transaction(config, &control)
        task = generate_task(config)
        columns = task[:schema].map do |column|
          Column.new(nil, column['name'], column['type'].to_sym, column['format'])
        end
        task[:soql] = generate_soql(task)
        resume(task, columns, 1, &control)
      end

      def self.resume(task, columns, count, &control)
        return yield(task, columns, count).first
      end

      def self.guess(config)
        task = generate_task(config)
        client = restforce_cli(task)
        soql = generate_soql(task)
        results = client.query("#{soql} LIMIT #{MAX_GUESS_RECORDS_NUM}")
        schema = Guess::SchemaGuess.from_hash_records(results.map(&:attrs))
        { 'columns' => schema }
      end

      def init
        @client = self.class.restforce_cli(task)
        @soql = task[:soql]
        @schema = task[:schema]
      end

      def run
        results = @client.query(@soql)
        Embulk.logger.info "Start to add records...(total #{results.count} records)"

        results.each do |result|
          values = @schema.collect { |column| generate_val(result, column) }
          page_builder.add(values)
        end

        page_builder.finish
      end

      def self.restforce_cli(task)
        Restforce.new({
          username: task[:username],
          password: task[:password],
          security_token: task[:security_token],
          client_id: task[:client_id],
          client_secret: task[:client_secret],
          instance_url: task[:instance_url],
          api_version: task[:api_version],
        })
      end

      private

      def generate_val(result, column)
        val = result[column['name']]
        if column['type'] == 'timestamp' && val
          begin
            val = Time.parse(val.to_s)
          rescue ArgumentError
            raise ConfigError.new "The value '#{val}' (as '#{column['name']}') is invalid time format"
          end
        elsif val.is_a?(Hash)
          val = val.to_s
        end
        val
      end

      def self.generate_task(config)
        {
          username: config.param('username', :string),
          password: config.param('password', :string),
          security_token: config.param('security_token', :string),
          client_id: config.param('client_id', :string),
          client_secret: config.param('client_secret', :string),
          instance_url: config.param('instance_url', :string),
          api_version: config.param('api_version', :string, default: '41.0'),
          soql: config.param('soql', :string),
          schema: config.param('columns', :array),
          conditions: config.param('conditions', :array)
        }
      end

      def self.generate_soql(task)
        soql = task[:soql]
        task[:conditions].each do |last_record|
          soql.gsub!(":#{last_record['key']}", last_record['value'])
        end
        soql
      end

      private_class_method :generate_task, :generate_soql
    end
  end
end
