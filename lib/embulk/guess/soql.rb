require 'json'

module Embulk
  module Guess
    class SoqlGuess < TextGuessPlugin
      Plugin.register_guess("soql", self)
      def guess_text(config, sample_text)
        {:columns =>
          begin
            SchemaGuess.from_hash_records(JSON.parse(sample_text)).map do |c|
              {
                name: c.name,
                type: c.type,
                **(c.format ? {format: c.format} : {})
              }
            end
          rescue => e
            Embulk.logger.info("[GUESS] skip sample text : #{sample_text}, config : #{config}, error : #{e}")
          end
        }
      end
    end
  end
end
